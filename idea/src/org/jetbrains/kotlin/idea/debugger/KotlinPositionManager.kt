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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.MultiRequestPositionManager
import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.PositionManagerEx
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ThreeState
import com.intellij.xdebugger.frame.XStackFrame
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest
import org.jetbrains.kotlin.codegen.inline.KOTLIN_STRATA_NAME
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.breakpoints.getLambdasAtLineIfAny
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinCodeFragmentFactory
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.refactoring.getLineCount
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import com.intellij.debugger.engine.DebuggerUtils as JDebuggerUtils

class KotlinPositionManager(private val myDebugProcess: DebugProcess) : MultiRequestPositionManager, PositionManagerEx() {

    private val scopes: List<GlobalSearchScope> = listOf(
            myDebugProcess.searchScope,
            KotlinSourceFilterScope.librarySources(GlobalSearchScope.allScope(myDebugProcess.project), myDebugProcess.project)
    )

    override fun evaluateCondition(context: EvaluationContext, frame: StackFrameProxyImpl, location: Location, expression: String): ThreeState? {
        return ThreeState.UNSURE
    }

    override fun createStackFrame(frame: StackFrameProxyImpl, debugProcess: DebugProcessImpl, location: Location): XStackFrame? {
        if (location.declaringType().containsKotlinStrata()) {
            return KotlinStackFrame(frame)
        }
        return null
    }

    override fun getSourcePosition(location: Location?): SourcePosition? {
        if (location == null) throw NoDataException.INSTANCE

        val psiFile = getPsiFileByLocation(location)
        if (psiFile == null) {
            val isKotlinStrataAvailable = location.declaringType().containsKotlinStrata()
            if (isKotlinStrataAvailable) {
                try {
                    val javaSourceFileName = location.sourceName("Java")
                    val javaClassName = JvmClassName.byInternalName(defaultInternalName(location))
                    val project = myDebugProcess.project

                    val defaultPsiFile = DebuggerUtils.findSourceFileForClass(project, scopes, javaClassName, javaSourceFileName)
                    if (defaultPsiFile != null) {
                        return SourcePosition.createFromLine(defaultPsiFile, 0)
                    }
                }
                catch(e: AbsentInformationException) {
                    // ignored
                }
            }

            throw NoDataException.INSTANCE
        }

        val sourceLineNumber = try {
            location.lineNumber() - 1
        }
        catch (e: InternalError) {
            -1
        }

        if (sourceLineNumber < 0) {
            throw NoDataException.INSTANCE
        }

        val lambdaOrFunIfInside = getLambdaOrFunIfInside(location, psiFile as KtFile, sourceLineNumber)
        if (lambdaOrFunIfInside != null) {
            return SourcePosition.createFromElement(lambdaOrFunIfInside.bodyExpression!!)
        }
        val elementInDeclaration = getElementForDeclarationLine(location, psiFile, sourceLineNumber)
        if (elementInDeclaration != null) {
            return SourcePosition.createFromElement(elementInDeclaration)
        }

        if (sourceLineNumber > psiFile.getLineCount() && myDebugProcess.isDexDebug()) {
            val (line, ktFile) = ktLocationInfo(location, true, myDebugProcess.project, false, psiFile)
            return SourcePosition.createFromLine(ktFile ?: psiFile, line - 1)
        }

        return SourcePosition.createFromLine(psiFile, sourceLineNumber)
    }

    // Returns a property or a constructor if debugger stops at class declaration
    private fun getElementForDeclarationLine(location: Location, file: KtFile, lineNumber: Int): KtElement? {
        val lineStartOffset = file.getLineStartOffset(lineNumber) ?: return null
        val elementAt = file.findElementAt(lineStartOffset)
        val contextElement = KotlinCodeFragmentFactory.getContextElement(elementAt)

        if (contextElement !is KtClass) return null

        val methodName = location.method().name()
        return when {
            JvmAbi.isGetterName(methodName) -> {
                val parameterForGetter = contextElement.primaryConstructor?.valueParameters?.firstOrNull {
                    it.hasValOrVar() && it.name != null && JvmAbi.getterName(it.name!!) == methodName
                } ?: return null
                parameterForGetter
            }
            methodName == "<init>" -> contextElement.primaryConstructor
            else -> null
        }
    }

    private fun getLambdaOrFunIfInside(location: Location, file: KtFile, lineNumber: Int): KtFunction? {
        val currentLocationFqName = location.declaringType().name() ?: return null

        val start = CodeInsightUtils.getStartLineOffset(file, lineNumber)
        val end = CodeInsightUtils.getEndLineOffset(file, lineNumber)
        if (start == null || end == null) return null

        val literalsOrFunctions = getLambdasAtLineIfAny(file, lineNumber)
        if (literalsOrFunctions.isEmpty()) return null

        val elementAt = file.findElementAt(start) ?: return null
        val typeMapper = KotlinDebuggerCaches.getOrCreateTypeMapper(elementAt)

        val currentLocationClassName = JvmClassName.byFqNameWithoutInnerClasses(FqName(currentLocationFqName))
                .internalName.replace('/', '.')

        for (literal in literalsOrFunctions) {
            if (InlineUtil.isInlinedArgument(literal, typeMapper.bindingContext, true)) {
                if (isInsideInlineArgument(literal, location, myDebugProcess as DebugProcessImpl)) {
                    return literal
                }
                continue
            }

            val internalClassNames = DebuggerClassNameProvider(myDebugProcess, scopes, alwaysReturnLambdaParentClass = false)
                    .getOuterClassNamesForElement(literal.firstChild)
                    .classNames

            if (internalClassNames.any { it == currentLocationClassName }) {
                return literal
            }
        }

        return null
    }

    private fun getPsiFileByLocation(location: Location): PsiFile? {
        val sourceName: String
        try {
            sourceName = location.sourceName()
        }
        catch (e: AbsentInformationException) {
            return null
        }
        catch (e: InternalError) {
            return null
        }

        val referenceInternalName = try {
            if (location.declaringType().containsKotlinStrata()) {
                //replace is required for windows
                location.sourcePath().replace('\\', '/')
            }
            else {
                defaultInternalName(location)
            }
        }
        catch (e: AbsentInformationException) {
            defaultInternalName(location)
        }

        val className = JvmClassName.byInternalName(referenceInternalName)

        val project = myDebugProcess.project

        return DebuggerUtils.findSourceFileForClass(project, scopes, className, sourceName)
    }

    private fun defaultInternalName(location: Location): String {
        //no stratum or source path => use default one
        val referenceFqName = location.declaringType().name()
        // JDI names are of form "package.Class$InnerClass"
        return referenceFqName.replace('.', '/')
    }

    override fun getAllClasses(sourcePosition: SourcePosition): List<ReferenceType> {
        val psiFile = sourcePosition.file
        if (psiFile is KtFile) {
            if (!ProjectRootsUtil.isInProjectOrLibSource(psiFile)) return emptyList()
            return DebuggerClassNameProvider(myDebugProcess, scopes).getClassesForPosition(sourcePosition)
        }

        if (psiFile is ClsFileImpl) {
            val decompiledPsiFile = psiFile.readAction { it.decompiledPsiFile }
            if (decompiledPsiFile is KtClsFile && sourcePosition.line == -1) {
                val className =
                        JvmFileClassUtil.getFileClassInfoNoResolve(decompiledPsiFile).fileClassFqName.internalNameWithoutInnerClasses
                return myDebugProcess.virtualMachineProxy.classesByName(className)
            }
        }

        throw NoDataException.INSTANCE
    }

    fun originalClassNamesForPosition(position: SourcePosition): List<String> {
        return DebuggerClassNameProvider(myDebugProcess, scopes, findInlineUseSites = false).getOuterClassNamesForPosition(position)
    }

    override fun locationsOfLine(type: ReferenceType, position: SourcePosition): List<Location> {
        if (position.file !is KtFile) {
            throw NoDataException.INSTANCE
        }
        try {
            if (myDebugProcess.isDexDebug()) {
                val inlineLocations = runReadAction { getLocationsOfInlinedLine(type, position, myDebugProcess.searchScope) }
                if (!inlineLocations.isEmpty()) {
                    return inlineLocations
                }
            }

            val line = position.line + 1

            val locations = type.locationsOfLine(KOTLIN_STRATA_NAME, null, line)
            if (locations == null || locations.isEmpty()) {
                throw NoDataException.INSTANCE
            }

            return locations.filter { it.sourceName(KOTLIN_STRATA_NAME) == position.file.name }
        }
        catch (e: AbsentInformationException) {
            throw NoDataException.INSTANCE
        }
    }

    @Deprecated("Since Idea 14.0.3 use createPrepareRequests fun")
    override fun createPrepareRequest(classPrepareRequestor: ClassPrepareRequestor, sourcePosition: SourcePosition): ClassPrepareRequest? {
        return createPrepareRequests(classPrepareRequestor, sourcePosition).firstOrNull()
    }

    override fun createPrepareRequests(requestor: ClassPrepareRequestor, position: SourcePosition): List<ClassPrepareRequest> {
        if (position.file !is KtFile) {
            throw NoDataException.INSTANCE
        }

        val classNames = DebuggerClassNameProvider(myDebugProcess, scopes).getOuterClassNamesForPosition(position)
        return classNames.mapNotNull { name ->
            myDebugProcess.requestsManager.createClassPrepareRequest(requestor, name + "*")
        }
    }

    private fun ReferenceType.containsKotlinStrata() = availableStrata().contains(KOTLIN_STRATA_NAME)
}

inline fun <U, V> U.readAction(crossinline f: (U) -> V): V {
    return runReadAction { f(this) }
}
