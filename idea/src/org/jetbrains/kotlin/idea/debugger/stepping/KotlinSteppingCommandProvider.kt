/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.MethodFilter
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.LocalVariable
import com.sun.jdi.Location
import com.sun.jdi.Method
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.codegen.inline.KOTLIN_STRATA_NAME
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.stepping.DexBytecode.GOTO
import org.jetbrains.kotlin.idea.debugger.stepping.DexBytecode.MOVE
import org.jetbrains.kotlin.idea.debugger.stepping.DexBytecode.RETURN
import org.jetbrains.kotlin.idea.debugger.stepping.DexBytecode.RETURN_OBJECT
import org.jetbrains.kotlin.idea.debugger.stepping.DexBytecode.RETURN_VOID
import org.jetbrains.kotlin.idea.debugger.stepping.DexBytecode.RETURN_WIDE
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.keysToMap

class KotlinSteppingCommandProvider : JvmSteppingCommandProvider() {
    override fun getStepOverCommand(
            suspendContext: SuspendContextImpl?,
            ignoreBreakpoints: Boolean,
            stepSize: Int
    ): DebugProcessImpl.ResumeCommand? {
        if (suspendContext == null || suspendContext.isResumed) return null

        val sourcePosition = suspendContext.debugProcess.debuggerContext.sourcePosition ?: return null
        return getStepOverCommand(suspendContext, ignoreBreakpoints, sourcePosition)
    }

    @TestOnly
    fun getStepOverCommand(
            suspendContext: SuspendContextImpl,
            ignoreBreakpoints: Boolean,
            debuggerContext: DebuggerContextImpl
    ): DebugProcessImpl.ResumeCommand? {
        return getStepOverCommand(suspendContext, ignoreBreakpoints, debuggerContext.sourcePosition)
    }

    private fun getStepOverCommand(
            suspendContext: SuspendContextImpl,
            ignoreBreakpoints: Boolean,
            sourcePosition: SourcePosition): DebugProcessImpl.ResumeCommand? {
        val kotlinSourcePosition = KotlinSourcePosition.create(sourcePosition) ?: return null

        if (isSpecialStepOverNeeded(kotlinSourcePosition)) {
            return DebuggerSteppingHelper.createStepOverCommand(suspendContext, ignoreBreakpoints, kotlinSourcePosition)
        }

        val file = sourcePosition.elementAt.containingFile
        val location = suspendContext.debugProcess.invokeInManagerThread { suspendContext.frameProxy?.location() } ?: return null
        if (isInSuspendMethod(location) && !isOnSuspendReturnOrReenter(location) && !isLastLineLocationInMethod(location)) {
            return DebugProcessImplHelper.createStepOverCommandWithCustomFilter(
                    suspendContext, ignoreBreakpoints, KotlinSuspendCallStepOverFilter(sourcePosition.line, file, ignoreBreakpoints))
        }

        return null
    }

    data class KotlinSourcePosition(val file: KtFile, val function: KtNamedFunction,
                                    val linesRange: IntRange, val sourcePosition: SourcePosition) {
        companion object {
            fun create(sourcePosition: SourcePosition): KotlinSourcePosition? {
                val file = sourcePosition.file as? KtFile ?: return null
                if (sourcePosition.line < 0) return null

                val containingFunction = sourcePosition.elementAt.parents
                                                 .filterIsInstance<KtNamedFunction>()
                                                 .firstOrNull { !it.isLocal } ?: return null

                val startLineNumber = containingFunction.getLineNumber(true) + 1
                val endLineNumber = containingFunction.getLineNumber(false) + 1
                if (startLineNumber > endLineNumber) return null

                val linesRange = startLineNumber..endLineNumber

                return KotlinSourcePosition(file, containingFunction, linesRange, sourcePosition)
            }
        }
    }

    private fun isSpecialStepOverNeeded(kotlinSourcePosition: KotlinSourcePosition): Boolean {
        val sourcePosition = kotlinSourcePosition.sourcePosition

        val hasInlineCallsOnLine = getInlineFunctionCallsIfAny(sourcePosition).isNotEmpty()
        if (hasInlineCallsOnLine) {
            return true
        }

        // Step over calls to lambda arguments in inline function while execution is already in that function
        val containingFunctionDescriptor = kotlinSourcePosition.function.unsafeResolveToDescriptor()
        if (InlineUtil.isInline(containingFunctionDescriptor)) {
            val inlineArgumentsCallsIfAny = getInlineArgumentsCallsIfAny(sourcePosition, containingFunctionDescriptor)
            if (inlineArgumentsCallsIfAny != null && inlineArgumentsCallsIfAny.isNotEmpty()) {
                return true
            }
        }

        return false
    }

    @TestOnly
    fun getStepOutCommand(suspendContext: SuspendContextImpl, debugContext: DebuggerContextImpl): DebugProcessImpl.ResumeCommand? {
        return getStepOutCommand(suspendContext, debugContext.sourcePosition)
    }

    override fun getStepOutCommand(suspendContext: SuspendContextImpl?, stepSize: Int): DebugProcessImpl.ResumeCommand? {
        if (suspendContext == null || suspendContext.isResumed) return null

        val sourcePosition = suspendContext.debugProcess.debuggerContext.sourcePosition ?: return null
        return getStepOutCommand(suspendContext, sourcePosition)
    }

    private fun getStepOutCommand(suspendContext: SuspendContextImpl, sourcePosition: SourcePosition): DebugProcessImpl.ResumeCommand? {
        val file = sourcePosition.file as? KtFile ?: return null
        if (sourcePosition.line < 0) return null

        val lineStartOffset = file.getLineStartOffset(sourcePosition.line) ?: return null

        val inlineFunctions = getInlineFunctionsIfAny(file, lineStartOffset)
        val inlinedArgument = getInlineArgumentIfAny(sourcePosition.elementAt)

        if (inlineFunctions.isEmpty() && inlinedArgument == null) return null

        return DebuggerSteppingHelper.createStepOutCommand(suspendContext, true, inlineFunctions, inlinedArgument)
    }
}

private fun PsiElement?.contains(element: PsiElement): Boolean {
    return this?.textRange?.contains(element.textRange) ?: false
}

private fun getInlineCallFunctionArgumentsIfAny(sourcePosition: SourcePosition): List<KtFunction> {
    val inlineFunctionCalls = getInlineFunctionCallsIfAny(sourcePosition)
    return getInlineArgumentsIfAny(inlineFunctionCalls)
}

private fun getInlineFunctionsIfAny(file: KtFile, offset: Int): List<KtNamedFunction> {
    val elementAt = file.findElementAt(offset) ?: return emptyList()
    val containingFunction = elementAt.getParentOfType<KtNamedFunction>(false) ?: return emptyList()

    val descriptor = containingFunction.unsafeResolveToDescriptor()
    if (!InlineUtil.isInline(descriptor)) return emptyList()

    return DebuggerUtils.analyzeElementWithInline(containingFunction, false).filterIsInstance<KtNamedFunction>()
}

private fun getInlineArgumentsIfAny(inlineFunctionCalls: List<KtCallExpression>): List<KtFunction> {
    return inlineFunctionCalls.flatMap {
        it.valueArguments
                .map(::getArgumentExpression)
                .filterIsInstance<KtFunction>()
    }
}

private fun getArgumentExpression(it: ValueArgument) = (it.getArgumentExpression() as? KtLambdaExpression)?.functionLiteral ?: it.getArgumentExpression()

private fun getInlineArgumentsCallsIfAny(sourcePosition: SourcePosition, declarationDescriptor: DeclarationDescriptor): List<KtCallExpression>? {
    if (declarationDescriptor !is CallableDescriptor) return null

    val valueParameters = declarationDescriptor.valueParameters.filter { it.type.isFunctionType }.toSet()

    if (valueParameters.isEmpty()) {
        return null
    }

    fun isCallOfArgument(ktCallExpression: KtCallExpression): Boolean {
        val context = ktCallExpression.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = ktCallExpression.getResolvedCall(context) as? VariableAsFunctionResolvedCallImpl ?: return false

        val candidateDescriptor = resolvedCall.variableCall.candidateDescriptor

        return candidateDescriptor in valueParameters
    }

    return findCallsOnPosition(sourcePosition, ::isCallOfArgument)
}

private fun getInlineFunctionCallsIfAny(sourcePosition: SourcePosition): List<KtCallExpression> {
    fun isInlineCall(expr: KtCallExpression): Boolean {
        val context = expr.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = expr.getResolvedCall(context) ?: return false
        return InlineUtil.isInline(resolvedCall.resultingDescriptor)
    }

    return findCallsOnPosition(sourcePosition, ::isInlineCall)
}

private fun findCallsOnPosition(sourcePosition: SourcePosition, filter: (KtCallExpression) -> Boolean): List<KtCallExpression> {
    val file = sourcePosition.file as? KtFile ?: return emptyList()
    val lineNumber = sourcePosition.line

    val lineElement = findElementAtLine(file, lineNumber)

    if (lineElement !is KtElement) {
        if (lineElement != null) {
            val call = findCallByEndToken(lineElement)
            if (call != null && filter(call)) {
                return listOf(call)
            }
        }

        return emptyList()
    }

    val start = lineElement.startOffset
    val end = lineElement.endOffset

    val allFilteredCalls = CodeInsightUtils.
            findElementsOfClassInRange(file, start, end, KtExpression::class.java)
            .map { KtPsiUtil.getParentCallIfPresent(it as KtExpression) }
            .filterIsInstance<KtCallExpression>()
            .filter { filter(it) }
            .toSet()

    // It is necessary to check range because of multiline assign
    var linesRange = lineNumber..lineNumber
    return allFilteredCalls.filter {
        val shouldInclude = it.getLineNumber() in linesRange
        if (shouldInclude) {
            linesRange = Math.min(linesRange.start, it.getLineNumber())..Math.max(linesRange.endInclusive, it.getLineNumber(false))
        }
        shouldInclude
    }
}

sealed class Action(val position: XSourcePositionImpl? = null,
                    val stepOverInlineData: StepOverFilterData? = null) {
    class STEP_OVER : Action() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) =
                debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints).contextAction(suspendContext)
    }
    class STEP_OUT : Action() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) =
            debugProcess.createStepOutCommand(suspendContext).contextAction(suspendContext)
    }
    class RUN_TO_CURSOR(position: XSourcePositionImpl) : Action(position) {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            return runReadAction {
                debugProcess.createRunToCursorCommand(suspendContext, position!!, ignoreBreakpoints)
            }.contextAction(suspendContext)
        }
    }
    class STEP_OVER_INLINED(stepOverInlineData: StepOverFilterData) : Action(stepOverInlineData = stepOverInlineData) {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            return KotlinStepActionFactory(debugProcess).createKotlinStepOverInlineAction(
                    KotlinStepOverInlineFilter(debugProcess.project, stepOverInlineData!!)).contextAction(suspendContext)
        }
    }

    abstract fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean)
}

interface KotlinMethodFilter : MethodFilter {
    fun locationMatches(context: SuspendContextImpl, location: Location): Boolean
}

fun getStepOverAction(
        location: Location,
        kotlinSourcePosition: KotlinSteppingCommandProvider.KotlinSourcePosition,
        frameProxy: StackFrameProxyImpl,
        isDexDebug: Boolean
): Action {
    val inlineArgumentsToSkip = runReadAction {
        getInlineCallFunctionArgumentsIfAny(kotlinSourcePosition.sourcePosition)
    }

    return getStepOverAction(location, kotlinSourcePosition.file, kotlinSourcePosition.linesRange,
                             inlineArgumentsToSkip, frameProxy, isDexDebug)
}

fun getStepOverAction(
        location: Location,
        sourceFile: KtFile,
        range: IntRange,
        inlineFunctionArguments: List<KtElement>,
        frameProxy: StackFrameProxyImpl,
        isDexDebug: Boolean
): Action {
    location.declaringType() ?: return Action.STEP_OVER()

    val project = sourceFile.project

    val methodLocations = location.method().allLineLocations()
    val locationsLineAndFile = methodLocations.keysToMap { ktLocationInfo(it, isDexDebug, project, true) }

    fun Location.ktLineNumber(): Int = (locationsLineAndFile[this] ?: ktLocationInfo(this, isDexDebug, project, true)).first
    fun Location.ktFileName(): String {
        val ktFile = (locationsLineAndFile[this] ?: ktLocationInfo(this, isDexDebug, project, true)).second
        // File is not null only for inlined locations. Get file name from debugger information otherwise.
        return ktFile?.name ?: this.sourceName(KOTLIN_STRATA_NAME)
    }

    fun isLocationSuitable(nextLocation: Location): Boolean {
        if (nextLocation.method() != location.method()) {
            return false
        }

        val ktLineNumber = nextLocation.ktLineNumber()
        if (ktLineNumber !in range) {
            return false
        }

        try {
            return nextLocation.ktFileName() == sourceFile.name
        }
        catch(e: AbsentInformationException) {
            return true
        }
    }

    fun isBackEdgeLocation(): Boolean {
        val previousSuitableLocation = methodLocations.reversed()
                .dropWhile { it != location }
                .drop(1)
                .filter(::isLocationSuitable)
                .dropWhile { it.ktLineNumber() == location.ktLineNumber() }
                .firstOrNull()

        return previousSuitableLocation != null && previousSuitableLocation.ktLineNumber() > location.ktLineNumber()
    }

    val patchedLocation = if (isBackEdgeLocation()) {
        // Pretend we had already done a backing step
        methodLocations
                .filter(::isLocationSuitable)
                .firstOrNull { it.ktLineNumber() == location.ktLineNumber() } ?: location
    }
    else {
        location
    }

    val patchedLineNumber = patchedLocation.ktLineNumber()

    val lambdaArgumentRanges = runReadAction {
        inlineFunctionArguments.map {
            val startLineNumber = it.getLineNumber(true) + 1
            val endLineNumber = it.getLineNumber(false) + 1

            startLineNumber..endLineNumber
        }
    }

    val inlineRangeVariables = getInlineRangeLocalVariables(frameProxy)

    // Try to find the range of inlined lines:
    // - Lines from other files and from functions that are not in range of current one are definitely inlined
    // - Lines in function arguments of inlined functions are inlined too as we found them starting from the position of inlined call.
    //
    // It also thinks that too many lines are inlined when there's a call of function argument or other
    // inline function in last statement of inline function. The list of inlineRangeVariables is used to overcome it.
    val probablyInlinedLocations = methodLocations
            .dropWhile { it != patchedLocation }
            .drop(1)
            .dropWhile { it.ktLineNumber() == patchedLineNumber }
            .takeWhile { loc ->
                !isLocationSuitable(loc) || lambdaArgumentRanges.any { loc.ktLineNumber() in it }
            }
            .dropWhile { it.ktLineNumber() == patchedLineNumber }

    if (!probablyInlinedLocations.isEmpty()) {
        // Some Kotlin inlined methods with 'for' (and maybe others) generates bytecode that after dexing have a strange artifact.
        // GOTO instructions are moved to the end of method and as they don't have proper line, line is obtained from the previous
        // instruction. It might be method return or previous GOTO from the inlining. Simple stepping over such function is really
        // terrible. On each iteration position jumps to the method end or some previous inline call and then returns back. To prevent
        // this filter locations with too big code indexes manually
        val returnCodeIndex: Long = if (isDexDebug) {
            val method = location.method()
            val locationsOfLine = method.locationsOfLine(range.last)
            if (locationsOfLine.isNotEmpty()) {
                locationsOfLine.map { it.codeIndex() }.max() ?: -1L
            }
            else {
                findReturnFromDexBytecode(location.method())
            }
        }
        else -1L

        return Action.STEP_OVER_INLINED(StepOverFilterData(
                patchedLineNumber,
                probablyInlinedLocations.map { it.ktLineNumber() }.toSet(),
                inlineRangeVariables,
                isDexDebug,
                returnCodeIndex
        ))
    }

    return Action.STEP_OVER()
}

fun getStepOutAction(
        location: Location,
        suspendContext: SuspendContextImpl,
        inlineFunctions: List<KtNamedFunction>,
        inlinedArgument: KtFunctionLiteral?
): Action {
    val computedReferenceType = location.declaringType() ?: return Action.STEP_OUT()

    val locations = computedReferenceType.allLineLocations()
    val nextLineLocations = locations
            .dropWhile { it != location }
            .drop(1)
            .filter { it.method() == location.method() }
            .dropWhile { it.lineNumber() == location.lineNumber() }

    if (inlineFunctions.isNotEmpty()) {
        val position = suspendContext.getXPositionForStepOutFromInlineFunction(nextLineLocations, inlineFunctions)
        return position?.let { Action.RUN_TO_CURSOR(it) } ?: Action.STEP_OVER()
    }

    if (inlinedArgument != null) {
        val position = suspendContext.getXPositionForStepOutFromInlinedArgument(nextLineLocations, inlinedArgument)
        return position?.let { Action.RUN_TO_CURSOR(it) } ?: Action.STEP_OVER()
    }

    return Action.STEP_OVER()
}

private fun SuspendContextImpl.getXPositionForStepOutFromInlineFunction(
        locations: List<Location>,
        inlineFunctionsToSkip: List<KtNamedFunction>
): XSourcePositionImpl? {
    return getNextPositionWithFilter(locations) {
        offset, elementAt ->
        if (inlineFunctionsToSkip.any { it.textRange.contains(offset) }) {
            return@getNextPositionWithFilter true
        }

        getInlineArgumentIfAny(elementAt) != null
    }
}

private fun SuspendContextImpl.getXPositionForStepOutFromInlinedArgument(
        locations: List<Location>,
        inlinedArgumentToSkip: KtFunctionLiteral
): XSourcePositionImpl? {
    return getNextPositionWithFilter(locations) {
        offset, _ ->
        inlinedArgumentToSkip.textRange.contains(offset)
    }
}

private fun SuspendContextImpl.getNextPositionWithFilter(
        locations: List<Location>,
        skip: (Int, PsiElement) -> Boolean
): XSourcePositionImpl? {
    for (location in locations) {
        val position = runReadAction l@ {
            val sourcePosition = try {
                this.debugProcess.positionManager.getSourcePosition(location)
            }
                                 catch(e: NoDataException) {
                                     null
                                 } ?: return@l null

            val file = sourcePosition.file as? KtFile ?: return@l null
            val elementAt = sourcePosition.elementAt ?: return@l null
            val currentLine = location.lineNumber() - 1
            val lineStartOffset = file.getLineStartOffset(currentLine) ?: return@l null
            if (skip(lineStartOffset, elementAt)) return@l null

            XSourcePositionImpl.createByElement(elementAt)
        }
        if (position != null) {
            return position
        }
    }

    return null
}

fun getInlineRangeLocalVariables(stackFrame: StackFrameProxyImpl): List<LocalVariable> {
    return stackFrame.visibleVariables()
            .filter {
                val name = it.name()
                name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION)
            }
            .map { it.variable }
}

private fun getInlineArgumentIfAny(elementAt: PsiElement?): KtFunctionLiteral? {
    val functionLiteralExpression = elementAt?.getParentOfType<KtLambdaExpression>(false) ?: return null

    val context = functionLiteralExpression.analyze(BodyResolveMode.PARTIAL)
    if (!InlineUtil.isInlinedArgument(functionLiteralExpression.functionLiteral, context, false)) return null

    return functionLiteralExpression.functionLiteral
}

private fun findReturnFromDexBytecode(method: Method): Long {
    val methodLocations = method.allLineLocations()
    if (methodLocations.isEmpty())  return -1L

    var lastMethodCodeIndex = methodLocations.last().codeIndex()
    // Continue while it's possible to get location
    while (true) {
        if (method.locationOfCodeIndex(lastMethodCodeIndex + 1) != null) {
            lastMethodCodeIndex++
        }
        else {
            break
        }
    }

    var returnIndex = lastMethodCodeIndex + 1

    val bytecode = method.bytecodes()
    var i = bytecode.size

    while (i >= 2) {
        // Can step only through two-byte instructions and abort on any unknown one
        i -= 2
        returnIndex -= 1

        val instruction = bytecode[i].toInt()

        if (instruction == RETURN_VOID || instruction == RETURN || instruction == RETURN_WIDE || instruction == RETURN_OBJECT) {
            // Instruction found
            return returnIndex
        }
        else if (instruction == MOVE || instruction == GOTO) {
            // proceed
        }
        else {
            // Don't know the instruction and it's length. Abort.
            break
        }
    }

    return -1L
}

object DexBytecode {
    val RETURN_VOID = 0x0e
    val RETURN = 0x0f
    val RETURN_WIDE = 0x10
    val RETURN_OBJECT = 0x11

    val GOTO = 0x28
    val MOVE = 0x01
}
