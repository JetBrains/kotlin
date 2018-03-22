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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.*
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.changeSignature.*
import com.intellij.refactoring.util.CanonicalTypes
import com.intellij.usageView.UsageInfo
import com.intellij.util.VisibilityUtil
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.builtins.isNonExtensionFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaOrKotlinMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.javaResolutionFacade
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor.Kind
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallerUsage
import org.jetbrains.kotlin.idea.refactoring.j2k
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

open class KotlinChangeInfo(
        val methodDescriptor: KotlinMethodDescriptor,
        private var name: String = methodDescriptor.name,
        var newReturnTypeInfo: KotlinTypeInfo = KotlinTypeInfo(true, methodDescriptor.baseDescriptor.returnType),
        var newVisibility: Visibility = methodDescriptor.visibility,
        parameterInfos: List<KotlinParameterInfo> = methodDescriptor.parameters,
        receiver: KotlinParameterInfo? = methodDescriptor.receiver,
        val context: PsiElement,
        primaryPropagationTargets: Collection<PsiElement> = emptyList()
) : ChangeInfo, UserDataHolder by UserDataHolderBase() {
    private class JvmOverloadSignature(
            val method: PsiMethod,
            val mandatoryParams: Set<KtParameter>,
            val defaultValues: Set<KtExpression>
    ) {
        fun constrainBy(other: JvmOverloadSignature): JvmOverloadSignature {
            return JvmOverloadSignature(method, mandatoryParams.intersect(other.mandatoryParams), defaultValues.intersect(other.defaultValues))
        }
    }

    private val originalReturnTypeInfo = methodDescriptor.returnTypeInfo
    private val originalReceiverTypeInfo = methodDescriptor.receiver?.originalTypeInfo

    var receiverParameterInfo: KotlinParameterInfo? = receiver
        set(value) {
            if (value != null && value !in newParameters) {
                newParameters.add(value)
            }
            field = value
        }

    private val newParameters = parameterInfos.toMutableList()

    private val originalPsiMethods = method.toLightMethods()
    private val originalParameters = (method as? KtFunction)?.valueParameters ?: emptyList()
    private val originalSignatures = makeSignatures(originalParameters, originalPsiMethods, { it }, { it.defaultValue })

    private val oldNameToParameterIndex: Map<String, Int> by lazy {
        val map = HashMap<String, Int>()

        val parameters = methodDescriptor.baseDescriptor.valueParameters
        parameters.indices.forEach { i -> map[parameters[i].name.asString()] = i }

        map
    }

    private val isParameterSetOrOrderChangedLazy: Boolean by lazy {
        val signatureParameters = getNonReceiverParameters()
        methodDescriptor.receiver != receiverParameterInfo ||
        signatureParameters.size != methodDescriptor.parametersCount ||
        signatureParameters.indices.any { i -> signatureParameters[i].oldIndex != i }
    }

    private var isPrimaryMethodUpdated: Boolean = false
    private var javaChangeInfos: List<JavaChangeInfo>? = null
    var originalToCurrentMethods: Map<PsiMethod, PsiMethod> = emptyMap()
        private set

    fun getOldParameterIndex(oldParameterName: String): Int? = oldNameToParameterIndex[oldParameterName]

    override fun isParameterTypesChanged(): Boolean = true

    override fun isParameterNamesChanged(): Boolean = true

    override fun isParameterSetOrOrderChanged(): Boolean = isParameterSetOrOrderChangedLazy

    fun getNewParametersCount(): Int = newParameters.size

    fun hasAppendedParametersOnly(): Boolean {
        val oldParamCount = originalBaseFunctionDescriptor.valueParameters.size
        return newParameters.withIndex().all { (i, p) -> if (i < oldParamCount) p.oldIndex == i else p.isNewParameter }
    }

    override fun getNewParameters(): Array<KotlinParameterInfo> = newParameters.toTypedArray()

    fun getNonReceiverParametersCount(): Int = newParameters.size - (if (receiverParameterInfo != null) 1 else 0)

    fun getNonReceiverParameters(): List<KotlinParameterInfo> {
        methodDescriptor.baseDeclaration.let { if (it is KtProperty || it is KtParameter) return emptyList() }
        return receiverParameterInfo?.let { receiver -> newParameters.filter { it != receiver } } ?: newParameters
    }

    fun setNewParameter(index: Int, parameterInfo: KotlinParameterInfo) {
        newParameters[index] = parameterInfo
    }

    @JvmOverloads fun addParameter(parameterInfo: KotlinParameterInfo, atIndex: Int = -1) {
        if (atIndex >= 0) {
            newParameters.add(atIndex, parameterInfo)
        }
        else {
            newParameters.add(parameterInfo)
        }
    }

    fun removeParameter(index: Int) {
        val parameterInfo = newParameters.removeAt(index)
        if (parameterInfo == receiverParameterInfo) {
            receiverParameterInfo = null
        }
    }

    fun clearParameters() {
        newParameters.clear()
        receiverParameterInfo = null
    }

    fun hasParameter(parameterInfo: KotlinParameterInfo): Boolean =
            parameterInfo in newParameters

    override fun isGenerateDelegate(): Boolean = false

    override fun getNewName(): String = name

    fun setNewName(value: String) {
        name = value
    }

    override fun isNameChanged(): Boolean = name != methodDescriptor.name

    fun isVisibilityChanged(): Boolean = newVisibility != methodDescriptor.visibility

    override fun getMethod(): PsiElement {
        return methodDescriptor.method
    }

    override fun isReturnTypeChanged(): Boolean = !newReturnTypeInfo.isEquivalentTo(originalReturnTypeInfo)

    fun isReceiverTypeChanged(): Boolean {
        val receiverInfo = receiverParameterInfo ?: return originalReceiverTypeInfo != null
        return originalReceiverTypeInfo == null || !receiverInfo.currentTypeInfo.isEquivalentTo(originalReceiverTypeInfo)
    }

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    var propagationTargetUsageInfos: List<UsageInfo> = ArrayList()
        private set

    var primaryPropagationTargets: Collection<PsiElement> = emptyList()
        set(value) {
            field = value

            val result = LinkedHashSet<UsageInfo>()

            fun add(element: PsiElement) {
                element.unwrapped?.let {
                    val usageInfo = when (it) {
                        is KtNamedFunction, is KtConstructor<*>, is KtClassOrObject ->
                            KotlinCallerUsage(it as KtNamedDeclaration)
                        is PsiMethod ->
                            CallerUsageInfo(it, true, true)
                        else ->
                            return
                    }

                    result.add(usageInfo)
                }
            }

            for (caller in value) {
                add(caller)
                OverridingMethodsSearch.search(caller.getRepresentativeLightMethod() ?: continue).forEach(::add)
            }

            propagationTargetUsageInfos = result.toList()
        }

    init {
        this.primaryPropagationTargets = primaryPropagationTargets
    }

    private fun renderReturnTypeIfNeeded(): String? {
        val typeInfo = newReturnTypeInfo
        if (kind != Kind.FUNCTION) return null
        if (typeInfo.type?.isUnit() ?: false) return null
        return typeInfo.render()
    }

    fun getNewSignature(inheritedCallable: KotlinCallableDefinitionUsage<PsiElement>): String {
        val buffer = StringBuilder()

        val defaultVisibility = if (kind.isConstructor) Visibilities.PUBLIC else Visibilities.DEFAULT_VISIBILITY

        if (kind == Kind.PRIMARY_CONSTRUCTOR) {
            buffer.append(name)

            if (newVisibility != defaultVisibility) {
                buffer.append(' ').append(newVisibility).append(" constructor ")
            }
        }
        else {
            if (!DescriptorUtils.isLocal(inheritedCallable.originalCallableDescriptor) && newVisibility != defaultVisibility) {
                buffer.append(newVisibility).append(' ')
            }

            buffer.append(if (kind == Kind.SECONDARY_CONSTRUCTOR) KtTokens.CONSTRUCTOR_KEYWORD else KtTokens.FUN_KEYWORD).append(' ')

            if (kind == Kind.FUNCTION) {
                receiverParameterInfo?.let {
                    val typeInfo = it.currentTypeInfo
                    if (typeInfo.type != null && typeInfo.type.isNonExtensionFunctionType) {
                        buffer.append("(${typeInfo.render()})")
                    }
                    else {
                        buffer.append(typeInfo.render())
                    }
                    buffer.append('.')
                }
            }

            buffer.append(name)
        }

        buffer.append(getNewParametersSignature(inheritedCallable))

        renderReturnTypeIfNeeded()?.let { buffer.append(": ").append(it) }

        return buffer.toString()
    }

    fun isRefactoringTarget(inheritedCallableDescriptor: CallableDescriptor?): Boolean {
        return inheritedCallableDescriptor != null
               && method == DescriptorToSourceUtils.descriptorToDeclaration(inheritedCallableDescriptor)
    }

    fun getNewParametersSignature(inheritedCallable: KotlinCallableDefinitionUsage<*>): String {
        return "(" + getNewParametersSignatureWithoutParentheses(inheritedCallable) + ")"
    }

    fun getNewParametersSignatureWithoutParentheses(
            inheritedCallable: KotlinCallableDefinitionUsage<*>
    ): String {
        val signatureParameters = getNonReceiverParameters()

        val isLambda = inheritedCallable.declaration is KtFunctionLiteral
        if (isLambda && signatureParameters.size == 1 && !signatureParameters[0].requiresExplicitType(inheritedCallable)) {
            return signatureParameters[0].getDeclarationSignature(0, inheritedCallable).text
        }

        return signatureParameters.indices.joinToString(separator = ", ") { i ->
            signatureParameters[i].getDeclarationSignature(i, inheritedCallable).text
        }
    }

    fun renderReceiverType(inheritedCallable: KotlinCallableDefinitionUsage<*>): String? {
        val receiverTypeText = receiverParameterInfo?.currentTypeInfo?.render() ?: return null
        val typeSubstitutor = inheritedCallable.typeSubstitutor ?: return receiverTypeText
        val currentBaseFunction = inheritedCallable.baseFunction.currentCallableDescriptor ?: return receiverTypeText
        val receiverType = currentBaseFunction.extensionReceiverParameter!!.type
        if (receiverType.isError) return receiverTypeText
        return receiverType.renderTypeWithSubstitution(typeSubstitutor, receiverTypeText, false)
    }

    fun renderReturnType(inheritedCallable: KotlinCallableDefinitionUsage<*>): String {
        val defaultRendering = newReturnTypeInfo.render()
        val typeSubstitutor = inheritedCallable.typeSubstitutor ?: return defaultRendering
        val currentBaseFunction = inheritedCallable.baseFunction.currentCallableDescriptor ?: return defaultRendering
        val returnType = currentBaseFunction.returnType!!
        if (returnType.isError) return defaultRendering
        return returnType.renderTypeWithSubstitution(typeSubstitutor, defaultRendering, false)
    }

    fun primaryMethodUpdated() {
        isPrimaryMethodUpdated = true
        javaChangeInfos = null
    }

    private fun <Parameter> makeSignatures(
            parameters: List<Parameter>,
            psiMethods: List<PsiMethod>,
            getPsi: (Parameter) -> KtParameter,
            getDefaultValue: (Parameter) -> KtExpression?
    ): List<JvmOverloadSignature> {
        val defaultValueCount = parameters.count { getDefaultValue(it) != null }
        if (psiMethods.size != defaultValueCount + 1) return emptyList()

        val mandatoryParams = parameters.toMutableList()
        val defaultValues = ArrayList<KtExpression>()
        return psiMethods.map {
            JvmOverloadSignature(it, mandatoryParams.map(getPsi).toSet(), defaultValues.toSet()).apply {
                val param = mandatoryParams.removeLast { getDefaultValue(it) != null } ?: return@apply
                defaultValues.add(getDefaultValue(param)!!)
            }
        }
    }

    private fun <T> MutableList<T>.removeLast(condition: (T) -> Boolean): T? {
        val index = indexOfLast(condition)
        return if (index >= 0) removeAt(index) else null
    }

    fun getOrCreateJavaChangeInfos(): List<JavaChangeInfo>? {
        fun initCurrentSignatures(currentPsiMethods: List<PsiMethod>): List<JvmOverloadSignature> {
            val parameterInfoToPsi = methodDescriptor.original.parameters.zip(originalParameters).toMap()
            val dummyParameter = KtPsiFactory(method).createParameter("dummy")
            return makeSignatures(newParameters,
                                  currentPsiMethods,
                                  { parameterInfoToPsi[it] ?: dummyParameter },
                                  { it.defaultValueForParameter })
        }

        fun matchOriginalAndCurrentMethods(currentPsiMethods: List<PsiMethod>): Map<PsiMethod, PsiMethod> {
            if (!(isPrimaryMethodUpdated
                  && originalBaseFunctionDescriptor is FunctionDescriptor
                  && originalBaseFunctionDescriptor.findJvmOverloadsAnnotation() != null)) {
                return (originalPsiMethods.zip(currentPsiMethods)).toMap()
            }

            if (originalPsiMethods.isEmpty() || currentPsiMethods.isEmpty()) return emptyMap()

            currentPsiMethods.singleOrNull()?.let { method -> return originalPsiMethods.keysToMap { method } }

            val currentSignatures = initCurrentSignatures(currentPsiMethods)
            return originalSignatures.associateBy({ it.method }) { originalSignature ->
                var constrainedCurrentSignatures = currentSignatures.map { it.constrainBy(originalSignature) }
                val maxMandatoryCount = constrainedCurrentSignatures.maxBy { it.mandatoryParams.size }!!.mandatoryParams.size
                constrainedCurrentSignatures = constrainedCurrentSignatures.filter { it.mandatoryParams.size == maxMandatoryCount }
                val maxDefaultCount = constrainedCurrentSignatures.maxBy { it.defaultValues.size }!!.defaultValues.size
                constrainedCurrentSignatures.last { it.defaultValues.size == maxDefaultCount }.method
            }
        }

        /*
         * When primaryMethodUpdated is false, changes to the primary Kotlin declaration are already confirmed, but not yet applied.
         * It means that originalPsiMethod has already expired, but new one can't be created until Kotlin declaration is updated
         * (signified by primaryMethodUpdated being true). It means we can't know actual PsiType, visibility, etc.
         * to use in JavaChangeInfo. However they are not actually used at this point since only parameter count and order matters here
         * So we resort to this hack and pass around "default" type (void) and visibility (package-local)
         */

        fun createJavaChangeInfo(originalPsiMethod: PsiMethod,
                                 currentPsiMethod: PsiMethod,
                                 newName: String,
                                 newReturnType: PsiType?,
                                 newParameters: Array<ParameterInfoImpl>
        ): JavaChangeInfo? {
            val newVisibility = if (isPrimaryMethodUpdated)
                VisibilityUtil.getVisibilityModifier(currentPsiMethod.modifierList)
            else
                PsiModifier.PACKAGE_LOCAL
            val propagationTargets = primaryPropagationTargets.asSequence()
                    .mapNotNull { it.getRepresentativeLightMethod() }
                    .toSet()
            val javaChangeInfo = ChangeSignatureProcessor(
                    method.project,
                    originalPsiMethod,
                    false,
                    newVisibility,
                    newName,
                    CanonicalTypes.createTypeWrapper(newReturnType ?: PsiType.VOID),
                    newParameters,
                    arrayOf<ThrownExceptionInfo>(),
                    propagationTargets,
                    emptySet()
            ).changeInfo
            javaChangeInfo.updateMethod(currentPsiMethod)

            return javaChangeInfo
        }

        fun getJavaParameterInfos(
                originalPsiMethod: PsiMethod,
                currentPsiMethod: PsiMethod,
                newParameterList: List<KotlinParameterInfo>
        ): MutableList<ParameterInfoImpl> {
            val defaultValuesToSkip = newParameterList.size - currentPsiMethod.parameterList.parametersCount
            val defaultValuesToRetain = newParameterList.count { it.defaultValueForParameter != null } - defaultValuesToSkip
            val oldIndices = newParameterList.map { it.oldIndex }.toIntArray()

            // TODO: Ugly code, need to refactor Change Signature data model
            var defaultValuesRemained = defaultValuesToRetain
            for (param in newParameterList) {
                if (param.isNewParameter || param.defaultValueForParameter == null || defaultValuesRemained-- > 0) continue
                newParameterList.withIndex().filter { it.value.oldIndex >= param.oldIndex }.forEach { oldIndices[it.index]-- }
            }

            defaultValuesRemained = defaultValuesToRetain
            val oldParameterCount = originalPsiMethod.parameterList.parametersCount
            var indexInCurrentPsiMethod = 0
            return newParameterList.withIndex()
                    .mapNotNullTo(ArrayList()) map@ { pair ->
                        val (i, info) = pair

                        if (info.defaultValueForParameter != null && defaultValuesRemained-- <= 0) return@map null

                        val oldIndex = oldIndices[i]
                        val javaOldIndex = when {
                            methodDescriptor.receiver == null -> oldIndex
                            info == methodDescriptor.receiver -> 0
                            oldIndex >= 0 -> oldIndex + 1
                            else -> -1
                        }
                        if (javaOldIndex >= oldParameterCount) return@map null

                        val type = if (isPrimaryMethodUpdated)
                            currentPsiMethod.parameterList.parameters[indexInCurrentPsiMethod++].type
                        else
                            PsiType.VOID

                        val defaultValue = info.defaultValueForCall ?: info.defaultValueForParameter
                        ParameterInfoImpl(javaOldIndex, info.name, type, defaultValue?.text ?: "")
                    }
        }

        fun createJavaChangeInfoForFunctionOrGetter(
                originalPsiMethod: PsiMethod,
                currentPsiMethod: PsiMethod,
                isGetter: Boolean
        ): JavaChangeInfo? {
            val newParameterList = listOfNotNull(receiverParameterInfo) + getNonReceiverParameters()
            val newJavaParameters = getJavaParameterInfos(originalPsiMethod, currentPsiMethod, newParameterList).toTypedArray()
            val newName = if (isGetter) JvmAbi.getterName(newName) else newName
            return createJavaChangeInfo(originalPsiMethod, currentPsiMethod, newName, currentPsiMethod.returnType, newJavaParameters)
        }

        fun createJavaChangeInfoForSetter(originalPsiMethod: PsiMethod, currentPsiMethod: PsiMethod): JavaChangeInfo? {
            val newJavaParameters = getJavaParameterInfos(originalPsiMethod, currentPsiMethod, listOfNotNull(receiverParameterInfo))
            val oldIndex = if (methodDescriptor.receiver != null) 1 else 0
            if (isPrimaryMethodUpdated) {
                val newIndex = if (receiverParameterInfo != null) 1 else 0
                val setterParameter = currentPsiMethod.parameterList.parameters[newIndex]
                newJavaParameters.add(ParameterInfoImpl(oldIndex, setterParameter.name, setterParameter.type))
            }
            else {
                newJavaParameters.add(ParameterInfoImpl(oldIndex, "receiver", PsiType.VOID))
            }

            val newName = JvmAbi.setterName(newName)
            return createJavaChangeInfo(originalPsiMethod, currentPsiMethod, newName, PsiType.VOID, newJavaParameters.toTypedArray())
        }

        if (TargetPlatformDetector.getPlatform(method.containingFile as KtFile) != JvmPlatform) return null

        if (javaChangeInfos == null) {
            val method = method
            originalToCurrentMethods = matchOriginalAndCurrentMethods(method.toLightMethods())
            javaChangeInfos = originalToCurrentMethods.entries.mapNotNull {
                val (originalPsiMethod, currentPsiMethod) = it

                when (method) {
                    is KtFunction, is KtClassOrObject ->
                        createJavaChangeInfoForFunctionOrGetter(originalPsiMethod, currentPsiMethod, false)
                    is KtProperty, is KtParameter -> {
                        val accessorName = originalPsiMethod.name
                        when {
                            JvmAbi.isGetterName(accessorName) ->
                                createJavaChangeInfoForFunctionOrGetter(originalPsiMethod, currentPsiMethod, true)
                            JvmAbi.isSetterName(accessorName) ->
                                createJavaChangeInfoForSetter(originalPsiMethod, currentPsiMethod)
                            else -> null
                        }
                    }
                    else -> null
                }
            }
        }

        return javaChangeInfos
    }
}

val KotlinChangeInfo.originalBaseFunctionDescriptor: CallableDescriptor
    get() = methodDescriptor.baseDescriptor

val KotlinChangeInfo.kind: Kind get() = methodDescriptor.kind

val KotlinChangeInfo.oldName: String?
    get() = (methodDescriptor.method as? KtFunction)?.name

fun KotlinChangeInfo.getAffectedCallables(): Collection<UsageInfo> = methodDescriptor.affectedCallables + propagationTargetUsageInfos

fun ChangeInfo.toJetChangeInfo(
        originalChangeSignatureDescriptor: KotlinMethodDescriptor,
        resolutionFacade: ResolutionFacade = method.javaResolutionFacade()
): KotlinChangeInfo {
    val method = method as PsiMethod

    val functionDescriptor = method.getJavaOrKotlinMemberDescriptor(resolutionFacade) as CallableDescriptor
    val parameterDescriptors = functionDescriptor.valueParameters

    //noinspection ConstantConditions
    val originalParameterDescriptors = originalChangeSignatureDescriptor.baseDescriptor.valueParameters

    val newParameters = newParameters.withIndex().map { pair ->
        val (i, info) = pair
        val oldIndex = info.oldIndex
        val currentType = parameterDescriptors[i].type

        val defaultValueText = info.defaultValue
        val defaultValueExpr =
                when {
                    info is KotlinAwareJavaParameterInfoImpl -> info.kotlinDefaultValue
                    language.`is`(JavaLanguage.INSTANCE) && !defaultValueText.isNullOrEmpty() -> {
                        PsiElementFactory.SERVICE.getInstance(method.project)
                                .createExpressionFromText(defaultValueText!!, null)
                                .j2k()
                    }
                    else -> null
                }

        val parameterType = if (oldIndex >= 0) originalParameterDescriptors[oldIndex].type else currentType
        val originalKtParameter = originalParameterDescriptors.getOrNull(oldIndex)?.source?.getPsi() as? KtParameter
        val valOrVar = originalKtParameter?.valOrVarKeyword?.toValVar() ?: KotlinValVar.None
        KotlinParameterInfo(callableDescriptor = functionDescriptor,
                            originalIndex = oldIndex,
                            name = info.name,
                            originalTypeInfo = KotlinTypeInfo(false, parameterType),
                            defaultValueForCall = defaultValueExpr,
                            valOrVar = valOrVar).apply {
            currentTypeInfo = KotlinTypeInfo(false, currentType)
        }
    }

    return KotlinChangeInfo(originalChangeSignatureDescriptor,
                            newName,
                            KotlinTypeInfo(true, functionDescriptor.returnType),
                            functionDescriptor.visibility,
                            newParameters,
                            null,
                            method)
}
