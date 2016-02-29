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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ThreeState
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassInternalName
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.breakpoints.getLambdasAtLineIfAny
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinCodeFragmentFactory
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.Companion.getOrComputeClassNames
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames.CachedClassNames
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames.NonCachedClassNames
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.util.DebuggerUtils
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*
import com.intellij.debugger.engine.DebuggerUtils as JDebuggerUtils

class KotlinPositionManager(private val myDebugProcess: DebugProcess) : MultiRequestPositionManager, PositionManagerEx() {

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
        if (location == null) {
            throw NoDataException.INSTANCE
        }

        val psiFile = getPsiFileByLocation(location)
        if (psiFile == null) {
            val isKotlinStrataAvailable = location.declaringType().containsKotlinStrata()
            if (isKotlinStrataAvailable) {
                try {
                    val javaSourceFileName = location.sourceName("Java")
                    val javaClassName = JvmClassName.byInternalName(defaultInternalName(location))
                    val project = myDebugProcess.project

                    val defaultPsiFile = DebuggerUtils.findSourceFileForClass(project, myDebugProcess.searchScope, javaClassName, javaSourceFileName)
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

        val lineNumber = try {
            location.lineNumber() - 1
        }
        catch (e: InternalError) {
            -1
        }


        if (lineNumber >= 0) {
            val lambdaOrFunIfInside = getLambdaOrFunIfInside(location, psiFile as KtFile, lineNumber)
            if (lambdaOrFunIfInside != null) {
                return SourcePosition.createFromElement(lambdaOrFunIfInside.bodyExpression!!)
            }
            val property = getParameterIfInConstructor(location, psiFile, lineNumber)
            if (property != null) {
                return SourcePosition.createFromElement(property)
            }
            return SourcePosition.createFromLine(psiFile, lineNumber)
        }

        throw NoDataException.INSTANCE
    }

    private fun getParameterIfInConstructor(location: Location, file: KtFile, lineNumber: Int): KtParameter? {
        val lineStartOffset = file.getLineStartOffset(lineNumber) ?: return null
        val elementAt = file.findElementAt(lineStartOffset)
        val contextElement = KotlinCodeFragmentFactory.getContextElement(elementAt)
        val methodName = location.method().name()
        if (contextElement is KtClass && JvmAbi.isGetterName(methodName)) {
            val parameterForGetter = contextElement.getPrimaryConstructor()?.valueParameters?.firstOrNull() {
                it.hasValOrVar() && it.name != null && JvmAbi.getterName(it.name!!) == methodName
            } ?: return null
            return parameterForGetter
        }
        return null
    }

    private fun getLambdaOrFunIfInside(location: Location, file: KtFile, lineNumber: Int): KtFunction? {
        val currentLocationFqName = location.declaringType().name() ?: return null

        val start = CodeInsightUtils.getStartLineOffset(file, lineNumber)
        val end = CodeInsightUtils.getEndLineOffset(file, lineNumber)
        if (start == null || end == null) return null

        val literalsOrFunctions = getLambdasAtLineIfAny(file, lineNumber)
        if (literalsOrFunctions.isEmpty()) return null;

        val elementAt = file.findElementAt(start) ?: return null
        val typeMapper = KotlinDebuggerCaches.getOrCreateTypeMapper(elementAt)

        val currentLocationClassName = JvmClassName.byFqNameWithoutInnerClasses(FqName(currentLocationFqName)).internalName
        for (literal in literalsOrFunctions) {
            if (InlineUtil.isInlinedArgument(literal, typeMapper.bindingContext, true)) {
                if (isInsideInlineArgument(literal, location, myDebugProcess as DebugProcessImpl)) {
                    return literal
                }
                continue
            }

            val internalClassNames = classNamesForPosition(literal.firstChild, true)
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


        val referenceInternalName: String
        try {
            if (location.declaringType().containsKotlinStrata()) {
                //replace is required for windows
                referenceInternalName = location.sourcePath().replace('\\', '/')
            }
            else {
                referenceInternalName = defaultInternalName(location)
            }
        }
        catch (e: AbsentInformationException) {
            referenceInternalName = defaultInternalName(location)
        }

        val className = JvmClassName.byInternalName(referenceInternalName)

        val project = myDebugProcess.project

        return DebuggerUtils.findSourceFileForClass(project, myDebugProcess.searchScope, className, sourceName)
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
            val result = ArrayList<ReferenceType>()

            if (!ProjectRootsUtil.isInProjectOrLibSource(psiFile)) return result

            val names = classNamesForPosition(sourcePosition, true)
            for (name in names) {
                result.addAll(myDebugProcess.virtualMachineProxy.classesByName(name))
            }
            return result
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

    fun originalClassNameForPosition(sourcePosition: SourcePosition): String? {
        return classNamesForPosition(sourcePosition, false).firstOrNull()
    }

    private fun classNamesForPosition(sourcePosition: SourcePosition, withInlines: Boolean): List<String> {
        val element = sourcePosition.readAction { it.elementAt } ?: return emptyList()
        val names = classNamesForPosition(element, withInlines)

        val lambdas = findLambdas(sourcePosition)
        if (lambdas.isEmpty()) {
            return names
        }

        return names + lambdas
    }

    private fun classNamesForPosition(element: PsiElement?, withInlines: Boolean): List<String> {
        if (DumbService.getInstance(myDebugProcess.project).isDumb) {
            return emptyList()
        }
        else {
            val baseElement = getElementToCalculateClassName(element) ?: return emptyList()
            return getOrComputeClassNames(baseElement) {
                element ->
                val file = element.readAction { it.containingFile as KtFile }
                val isInLibrary = LibraryUtil.findLibraryEntry(file.virtualFile, file.project) != null
                val typeMapper = KotlinDebuggerCaches.getOrCreateTypeMapper(element)

                getInternalClassNameForElement(element, typeMapper, file, isInLibrary, withInlines)
            }
        }
    }

    private fun findLambdas(sourcePosition: SourcePosition): Collection<String> {
        val lambdas = sourcePosition.readAction { getLambdasAtLineIfAny(it) }
        return lambdas.flatMap { classNamesForPosition(it, true) }
    }

    override fun locationsOfLine(type: ReferenceType, position: SourcePosition): List<Location> {
        if (position.file !is KtFile) {
            throw NoDataException.INSTANCE
        }
        try {
            val line = position.line + 1
            val locations = if (myDebugProcess.virtualMachineProxy.versionHigher("1.4"))
                type.locationsOfLine("Kotlin", null, line).filter { it.sourceName("Kotlin") == position.file.name }
            else
                type.locationsOfLine(line)
            if (locations == null || locations.isEmpty()) throw NoDataException.INSTANCE
            return locations
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

        return classNamesForPosition(position, true).mapNotNull {
            className ->
            myDebugProcess.requestsManager.createClassPrepareRequest(requestor, className.replace('/', '.'))
        }
    }

    private fun getInternalClassNameForElement(
            element: KtElement,
            typeMapper: KotlinTypeMapper,
            file: KtFile,
            isInLibrary: Boolean,
            withInlines: Boolean
    ): KotlinDebuggerCaches.ComputedClassNames {
        val parent = element.readAction { getElementToCalculateClassName(it.parent) }
        when (element) {
            is KtClassOrObject -> return CachedClassNames(getClassNameForClass(element, typeMapper))
            is KtFunction -> {
                val descriptor = element.readAction { InlineUtil.getInlineArgumentDescriptor(it, typeMapper.bindingContext) }
                if (descriptor != null) {
                    val classNamesForParent = classNamesForPosition(parent, withInlines)
                    if (descriptor.isCrossinline) {
                        return CachedClassNames(classNamesForParent + findCrossInlineArguments(element, descriptor, typeMapper.bindingContext))
                    }
                    return CachedClassNames(classNamesForParent)
                }
            }
        }

        val crossInlineParameterUsages = element.readAction { it.containsCrossInlineParameterUsages(typeMapper.bindingContext) }
        if (crossInlineParameterUsages.isNotEmpty()) {
            return CachedClassNames(classNamesForCrossInlineParameters(crossInlineParameterUsages, typeMapper.bindingContext).toList())
        }

        when {
            element is KtFunctionLiteral -> {
                val asmType = CodegenBinding.asmTypeForAnonymousClass(typeMapper.bindingContext, element)
                return CachedClassNames(asmType.internalName)
            }
            element is KtAnonymousInitializer -> {
                // Class-object initializer
                if (parent is KtObjectDeclaration && parent.isCompanion()) {
                    return CachedClassNames(classNamesForPosition(parent.parent, withInlines))
                }
                return CachedClassNames(classNamesForPosition(parent, withInlines))
            }
            element is KtPropertyAccessor && (!element.readAction { it.property.isTopLevel } || !isInLibrary) -> {
                val classOrObject = element.readAction { PsiTreeUtil.getParentOfType(it, KtClassOrObject::class.java) }
                if (classOrObject != null) {
                    return CachedClassNames(getClassNameForClass(classOrObject, typeMapper))
                }
            }
            element is KtProperty && (!element.readAction { it.isTopLevel } || !isInLibrary) -> {
                val descriptor = typeMapper.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
                if (descriptor !is PropertyDescriptor) {
                    return CachedClassNames(classNamesForPosition(parent, withInlines))
                }

                return CachedClassNames(getJvmInternalNameForPropertyOwner(typeMapper, descriptor))
            }
            element is KtNamedFunction -> {
                val parentInternalName = if (parent is KtClassOrObject) {
                    getClassNameForClass(parent, typeMapper)
                }
                else if (parent != null) {
                    val asmType = CodegenBinding.asmTypeForAnonymousClass(typeMapper.bindingContext, element)
                    asmType.internalName
                }
                else {
                    getClassNameForFile(file)
                }

                if (!withInlines) return NonCachedClassNames(parentInternalName)

                val inlinedCalls = findInlinedCalls(element, typeMapper.bindingContext)
                if (parentInternalName == null) return CachedClassNames(inlinedCalls)

                return CachedClassNames(listOf(parentInternalName) + inlinedCalls)

            }
        }

        return CachedClassNames(getClassNameForFile(file))
    }

    private fun getClassNameForClass(klass: KtClassOrObject, typeMapper: KotlinTypeMapper) = klass.readAction { getJvmInternalNameForImpl(typeMapper, it) }
    private fun getClassNameForFile(file: KtFile) = file.readAction { NoResolveFileClassesProvider.getFileClassInternalName(it) }

    private val TYPES_TO_CALCULATE_CLASSNAME: Array<Class<out KtElement>> =
            arrayOf(KtClass::class.java,
                    KtObjectDeclaration::class.java,
                    KtEnumEntry::class.java,
                    KtFunctionLiteral::class.java,
                    KtNamedFunction::class.java,
                    KtPropertyAccessor::class.java,
                    KtProperty::class.java,
                    KtClassInitializer::class.java)

    private fun getElementToCalculateClassName(notPositionedElement: PsiElement?): KtElement? {
        if (notPositionedElement?.javaClass as Class<*> in TYPES_TO_CALCULATE_CLASSNAME) return notPositionedElement as KtElement

        return readAction { PsiTreeUtil.getParentOfType(notPositionedElement, *TYPES_TO_CALCULATE_CLASSNAME) }
    }

    fun getJvmInternalNameForPropertyOwner(typeMapper: KotlinTypeMapper, descriptor: PropertyDescriptor): String {
        return descriptor.readAction {
            typeMapper.mapOwner(
                    if (JvmAbi.isPropertyWithBackingFieldInOuterClass(it)) it.containingDeclaration else it
            ).internalName
        }
    }

    private fun getJvmInternalNameForImpl(typeMapper: KotlinTypeMapper, ktClass: KtClassOrObject): String? {
        val classDescriptor = typeMapper.bindingContext.get<PsiElement, ClassDescriptor>(BindingContext.CLASS, ktClass) ?: return null

        if (ktClass is KtClass && ktClass.isInterface()) {
            return typeMapper.mapDefaultImpls(classDescriptor).internalName
        }

        return typeMapper.mapClass(classDescriptor).internalName
    }

    private fun findInlinedCalls(function: KtNamedFunction, context: BindingContext): List<String> {
        if (!InlineUtil.isInline(context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function))) {
            return emptyList()
        }
        else {
            val searchResult = hashSetOf<KtElement>()
            val functionName = function.readAction { it.name }

            val task = Runnable {
                ReferencesSearch.search(function, myDebugProcess.searchScope).forEach {
                    if (!it.readAction { it.isImportUsage() }) {
                        val usage = (it.element as? KtElement)?.let { getElementToCalculateClassName(it) }
                        if (usage != null) {
                            searchResult.add(usage)
                        }
                    }
                }
            }

            var isSuccess = true
            ApplicationManager.getApplication().invokeAndWait(
                    {
                       isSuccess = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                                task,
                                "Compute class names for function $functionName",
                                true,
                                myDebugProcess.project)
                    }, ModalityState.NON_MODAL)

            if (!isSuccess) {
                XDebugSessionImpl.NOTIFICATION_GROUP.createNotification(
                        "Debugger can skip some executions of $functionName method, because the computation of class names was interrupted", MessageType.WARNING
                ).notify(myDebugProcess.project)
            }

            // TODO recursive search
            return searchResult.flatMap { classNamesForPosition(it, true) }
        }
    }

    private fun findCrossInlineArguments(argument: KtFunction, parameterDescriptor: ValueParameterDescriptor, context: BindingContext): Set<String> {
        return runReadAction {
            val source = parameterDescriptor.source.getPsi() as? KtParameter
            val functionName = source?.ownerFunction?.name
            if (functionName != null) {
                return@runReadAction setOf(getCrossInlineArgumentClassName(argument, functionName, context))
            }
            return@runReadAction emptySet()
        }
    }

    private fun getCrossInlineArgumentClassName(argument: KtFunction, inlineFunctionName: String, context: BindingContext): String {
        val anonymousClassNameForArgument = CodegenBinding.asmTypeForAnonymousClass(context, argument).internalName
        val newName = anonymousClassNameForArgument.substringIndex() + InlineCodegenUtil.INLINE_TRANSFORMATION_SUFFIX + "$" + inlineFunctionName
        return "$newName$*"
    }

    private fun KtElement.containsCrossInlineParameterUsages(context: BindingContext): Collection<ValueParameterDescriptor> {
        fun KtElement.hasParameterCall(parameter: KtParameter): Boolean {
            return ReferencesSearch.search(parameter).any {
                this.textRange.contains(it.element.textRange)
            }
        }

        val inlineFunction = this.parents.firstIsInstanceOrNull<KtNamedFunction>() ?: return emptySet()

        val inlineFunctionDescriptor = context[BindingContext.FUNCTION, inlineFunction]
        if (inlineFunctionDescriptor == null || !InlineUtil.isInline(inlineFunctionDescriptor)) return emptySet()

        return inlineFunctionDescriptor.valueParameters
                .filter { it.isCrossinline }
                .mapNotNull {
                    val psiParameter = it.source.getPsi() as? KtParameter
                    if (psiParameter != null && this@containsCrossInlineParameterUsages.hasParameterCall(psiParameter))
                        it
                    else
                        null
                }
    }

    private fun classNamesForCrossInlineParameters(usedParameters: Collection<ValueParameterDescriptor>, context: BindingContext): Set<String> {
        // We could calculate className only for one of parameters, because we add '*' to match all crossInlined parameter calls
        val parameter = usedParameters.first()
        val result = hashSetOf<String>()
        val inlineFunction = parameter.containingDeclaration.source.getPsi() as? KtNamedFunction ?: return emptySet()

        ReferencesSearch.search(inlineFunction, myDebugProcess.searchScope).forEach {
            runReadAction {
                if (!it.isImportUsage()) {
                    val call = (it.element as? KtExpression)?.let { KtPsiUtil.getParentCallIfPresent(it) }
                    if (call != null) {
                        val resolvedCall = call.getResolvedCall(context)
                        val argument = resolvedCall?.valueArguments?.entries?.firstOrNull { it.key.original == parameter }?.value
                        if (argument != null) {
                            val argumentExpression = getArgumentExpression(argument.arguments.first())
                            if (argumentExpression is KtFunction) {
                                result.add(getCrossInlineArgumentClassName(argumentExpression, inlineFunction.name!!, context))
                            }
                        }
                    }
                }
            }
        }

        return result
    }

    private fun getArgumentExpression(it: ValueArgument) = (it.getArgumentExpression() as? KtLambdaExpression)?.functionLiteral ?: it.getArgumentExpression()

    private fun String.substringIndex(): String {
        if (lastIndexOf("$") < 0) return this

        val suffix = substringAfterLast("$")
        if (suffix.all { it.isDigit() }) {
            return substringBeforeLast("$") + "$"
        }
        return this
    }

    private fun ReferenceType.containsKotlinStrata() = availableStrata().contains("Kotlin")
}

private inline fun <U, V> U.readAction(crossinline f: (U) -> V): V {
    return runReadAction { f(this) }
}
