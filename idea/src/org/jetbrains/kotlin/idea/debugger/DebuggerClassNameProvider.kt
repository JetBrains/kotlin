/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegen
import org.jetbrains.kotlin.codegen.coroutines.DO_RESUME_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.containsNonTailSuspensionCalls
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassInternalName
import org.jetbrains.kotlin.idea.debugger.breakpoints.getLambdasAtLineIfAny
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.Companion.getOrComputeClassNames
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames.CachedClassNames
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames.NonCachedClassNames
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull


class DebuggerClassNameProvider(val myDebugProcess: DebugProcess, val scopes: List<GlobalSearchScope>) {
    fun classNamesForPosition(sourcePosition: SourcePosition, withInlines: Boolean): List<String> {
        val element = sourcePosition.readAction { it.elementAt } ?: return emptyList()
        val names = classNamesForPosition(element, withInlines)

        val lambdas = findLambdas(sourcePosition)
        if (lambdas.isEmpty()) {
            return names
        }

        return names + lambdas
    }

    fun classNamesForPosition(element: PsiElement?, withInlines: Boolean): List<String> {
        if (DumbService.getInstance(myDebugProcess.project).isDumb) {
            return emptyList()
        }

        val baseElement = getElementToCalculateClassName(element) ?: return emptyList()
        return getOrComputeClassNames(baseElement) { element ->
            val file = element.readAction { it.containingFile as KtFile }
            val isInLibrary = LibraryUtil.findLibraryEntry(file.virtualFile, file.project) != null
            val typeMapper = KotlinDebuggerCaches.getOrCreateTypeMapper(element)

            getInternalClassNameForElement(element, typeMapper, file, isInLibrary, withInlines)
        }
    }

    private fun findLambdas(sourcePosition: SourcePosition): Collection<String> {
        val lambdas = sourcePosition.readAction(::getLambdasAtLineIfAny)
        return lambdas.flatMap { classNamesForPosition(it, true) }
    }


    private fun getInternalClassNameForElement(
            element: KtElement,
            typeMapper: KotlinTypeMapper,
            file: KtFile,
            isInLibrary: Boolean,
            withInlines: Boolean
    ): KotlinDebuggerCaches.ComputedClassNames {
        val elementOfClassName = element.readAction { getElementToCalculateClassName(it.parent) }
        when (element) {
            is KtClassOrObject -> return CachedClassNames(getClassNameForClass(element, typeMapper))
            is KtSecondaryConstructor -> {
                val klass = element.readAction { it.getContainingClassOrObject() }
                return CachedClassNames(getClassNameForClass(klass, typeMapper))
            }
            is KtFunction -> {
                val descriptor = element.readAction { InlineUtil.getInlineArgumentDescriptor(it, typeMapper.bindingContext) }
                if (descriptor != null) {
                    val classNamesForParent = classNamesForPosition(elementOfClassName, withInlines)
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
                val className = asmType.internalName

                val inlineCallClassPatterns = inlineCallClassPatterns(typeMapper, element)
                val names = listOf(className) + inlineCallClassPatterns

                return CachedClassNames(names)
            }
            element is KtAnonymousInitializer -> {
                // Class-object initializer
                if (elementOfClassName is KtObjectDeclaration && elementOfClassName.isCompanion()) {
                    return CachedClassNames(classNamesForPosition(elementOfClassName.parent, withInlines))
                }
                return CachedClassNames(classNamesForPosition(elementOfClassName, withInlines))
            }
            element is KtPropertyAccessor && (!element.readAction { it.property.isTopLevel } || !isInLibrary) -> {
                val classOrObject = element.readAction { PsiTreeUtil.getParentOfType(it, KtClassOrObject::class.java) }
                if (classOrObject != null) {
                    return CachedClassNames(getClassNameForClass(classOrObject, typeMapper))
                }
            }
            element is KtProperty && (!element.readAction { it.isTopLevel } || !isInLibrary) -> {
                val descriptor = typeMapper.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element) as? PropertyDescriptor ?:
                                 return CachedClassNames(classNamesForPosition(elementOfClassName, withInlines))

                return CachedClassNames(getJvmInternalNameForPropertyOwner(typeMapper, descriptor))
            }
            element is KtNamedFunction -> {
                val descriptor = typeMapper.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)

                val parentInternalName = when {
                    isFunctionWithSuspendStateMachine(descriptor, typeMapper.bindingContext) -> {
                        CodegenBinding.asmTypeForAnonymousClass(typeMapper.bindingContext, element).internalName
                    }
                    elementOfClassName is KtClassOrObject -> getClassNameForClass(elementOfClassName, typeMapper)
                    elementOfClassName != null -> {
                        val asmType = CodegenBinding.asmTypeForAnonymousClass(typeMapper.bindingContext, element)
                        asmType.internalName
                    }
                    else -> {
                        getClassNameForFile(file)
                    }
                }

                if (!withInlines) return NonCachedClassNames(parentInternalName)
                val inlinedCalls = findInlinedCalls(element, typeMapper.bindingContext)
                if (parentInternalName == null) return CachedClassNames(inlinedCalls)

                val inlineCallPatterns = inlineCallClassPatterns(typeMapper, element)

                return CachedClassNames((listOf(parentInternalName) + inlinedCalls + inlineCallPatterns).filterNotNull())
            }
        }

        return CachedClassNames(getClassNameForFile(file))
    }

    private fun isFunctionWithSuspendStateMachine(descriptor: DeclarationDescriptor?, bindingContext: BindingContext): Boolean {
        return descriptor is SimpleFunctionDescriptor && descriptor.isSuspend && descriptor.containsNonTailSuspensionCalls(bindingContext)
    }

    private fun inlineCallClassPatterns(typeMapper: KotlinTypeMapper, element: KtElement): List<String> {
        val context = typeMapper.bindingContext

        val inlineCall = runReadAction {
            element.parents.map {
                val ktCallExpression: KtCallExpression = when(it) {
                    is KtFunctionLiteral -> {
                        val lambdaExpression = it.parent as? KtLambdaExpression
                        // call(param, { <it> })
                        lambdaExpression?.typedParent<KtValueArgument>()?.typedParent<KtValueArgumentList>()?.typedParent<KtCallExpression>() ?:

                        // call { <it> }
                        lambdaExpression?.typedParent<KtLambdaArgument>()?.typedParent<KtCallExpression>()
                    }

                    is KtNamedFunction -> {
                        // call(fun () {})
                        it.typedParent<KtValueArgument>()?.typedParent<KtValueArgumentList>()?.typedParent<KtCallExpression>()
                    }

                    else -> null
                } ?: return@map null

                ktCallExpression to (it as KtElement)
            }.lastOrNull {
                it != null && isInlineCall(context, it.component1())
            }?.first
        } ?: return emptyList()

        val lexicalScope = context[BindingContext.LEXICAL_SCOPE, inlineCall] ?: return emptyList()
        val baseClassName = classNamesForPosition(inlineCall, false).firstOrNull() ?: return emptyList()

        val resolvedCall = runReadAction { inlineCall.getResolvedCall(context) } ?: return emptyList()
        val inlineFunctionName = resolvedCall.resultingDescriptor.name

        val ownerDescriptor = lexicalScope.ownerDescriptor
        val ownerDeclaration = if (ownerDescriptor is DeclarationDescriptor) DescriptorToSourceUtils.getSourceFromDescriptor(ownerDescriptor) else null

        val ownerDescriptorName =
                if (isFunctionWithSuspendStateMachine(ownerDescriptor, typeMapper.bindingContext) ||
                    (ownerDescriptor is CallableDescriptor && ownerDeclaration is KtElement && CoroutineCodegen.shouldCreateByLambda(ownerDescriptor, ownerDeclaration))) {
                    Name.identifier(DO_RESUME_METHOD_NAME)
                }
                else {
                    ownerDescriptor.name
                }

        val ownerJvmName = if (ownerDescriptorName.isSpecial) InlineCodegenUtil.SPECIAL_TRANSFORMATION_NAME else ownerDescriptorName.asString()
        val mangledInternalClassName =
                baseClassName + "$" + ownerJvmName + InlineCodegenUtil.INLINE_CALL_TRANSFORMATION_SUFFIX + "$" + inlineFunctionName

        return listOf("$mangledInternalClassName*")
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
                    KtClassInitializer::class.java,
                    KtSecondaryConstructor::class.java)

    private fun getElementToCalculateClassName(notPositionedElement: PsiElement?): KtElement? {
        if (notPositionedElement?.let { it::class.java } as Class<*> in TYPES_TO_CALCULATE_CLASSNAME) return notPositionedElement as KtElement

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
                ReferencesSearch.search(function, getScopeForInlineFunctionUsages(function)).forEach {
                    if (!it.readAction { it.isImportUsage() }) {
                        val usage = (it.element as? KtElement)?.let { getElementToCalculateClassName(it) }
                        if (usage != null) {
                            searchResult.add(usage)
                        }
                    }
                }
            }

            var isSuccess = true
            val applicationEx = ApplicationManagerEx.getApplicationEx()
            if (!applicationEx.isUnitTestMode && (!applicationEx.holdsReadLock() || applicationEx.isDispatchThread)) {
                applicationEx.invokeAndWait(
                        {
                            isSuccess = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                                    task,
                                    "Compute class names for function $functionName",
                                    true,
                                    myDebugProcess.project)
                        }, ModalityState.NON_MODAL)
            }
            else {
                // Pooled thread with read lock. Can't invoke task under UI progress, so call it directly.
                task.run()
            }

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
            val source = parameterDescriptor.original.source.getPsi() as? KtParameter
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

        ReferencesSearch.search(inlineFunction, getScopeForInlineFunctionUsages(inlineFunction)).forEach {
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

    private fun getScopeForInlineFunctionUsages(inlineFunction: KtNamedFunction): GlobalSearchScope {
        val virtualFile = runReadAction { inlineFunction.containingFile.virtualFile }
        if (virtualFile != null && ProjectRootsUtil.isLibraryFile(myDebugProcess.project, virtualFile)) {
            return GlobalSearchScope.union(scopes.toTypedArray())
        }
        else {
            return myDebugProcess.searchScope
        }
    }

    private fun getArgumentExpression(it: ValueArgument) = (it.getArgumentExpression() as? KtLambdaExpression)?.functionLiteral ?: it.getArgumentExpression()
}

private fun isInlineCall(context: BindingContext, expr: KtCallExpression): Boolean {
    val resolvedCall = expr.getResolvedCall(context) ?: return false
    return InlineUtil.isInline(resolvedCall.resultingDescriptor)
}

private inline fun <reified T : PsiElement> PsiElement.typedParent(): T? = getStrictParentOfType()

private fun String.substringIndex(): String {
    if (lastIndexOf("$") < 0) return this

    val suffix = substringAfterLast("$")
    if (suffix.all(Char::isDigit)) {
        return substringBeforeLast("$") + "$"
    }
    return this
}