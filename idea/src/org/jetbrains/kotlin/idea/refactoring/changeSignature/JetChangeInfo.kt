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
import com.intellij.psi.*
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.changeSignature.*
import com.intellij.refactoring.util.CanonicalTypes
import com.intellij.usageView.UsageInfo
import com.intellij.util.VisibilityUtil
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.core.refactoring.j2k
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetMethodDescriptor.Kind
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JetCallableDefinitionUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallerUsage
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmOverloadsAnnotation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.utils.removeLast
import java.util.*

public open class JetChangeInfo(
        val methodDescriptor: JetMethodDescriptor,
        private var name: String = methodDescriptor.getName(),
        val newReturnType: KotlinType? = methodDescriptor.baseDescriptor.getReturnType(),
        var newReturnTypeText: String = methodDescriptor.renderOriginalReturnType(),
        var newVisibility: Visibility = methodDescriptor.getVisibility(),
        parameterInfos: List<JetParameterInfo> = methodDescriptor.getParameters(),
        receiver: JetParameterInfo? = methodDescriptor.receiver,
        val context: PsiElement,
        primaryPropagationTargets: Collection<PsiElement> = emptyList()
) : ChangeInfo {
    private class JvmOverloadSignature(
            val method: PsiMethod,
            val mandatoryParams: Set<KtParameter>,
            val defaultValues: Set<KtExpression>
    ) {
        fun constrainBy(other: JvmOverloadSignature): JvmOverloadSignature {
            return JvmOverloadSignature(method, mandatoryParams.intersect(other.mandatoryParams), defaultValues.intersect(other.defaultValues))
        }
    }

    var receiverParameterInfo: JetParameterInfo? = receiver
        set(value: JetParameterInfo?) {
            if (value != null && value !in newParameters) {
                newParameters.add(value)
            }
            field = value
        }

    private val newParameters = parameterInfos.toArrayList()

    private val originalPsiMethods = method.toLightMethods()
    private val originalParameters = (method as? KtFunction)?.valueParameters ?: emptyList()
    private val originalSignatures = makeSignatures(originalParameters, originalPsiMethods, { it }, { it.defaultValue })

    private val oldNameToParameterIndex: Map<String, Int> by lazy {
        val map = HashMap<String, Int>()

        val parameters = methodDescriptor.baseDescriptor.getValueParameters()
        parameters.indices.forEach { i -> map[parameters.get(i).getName().asString()] = i }

        map
    }

    private val isParameterSetOrOrderChangedLazy: Boolean by lazy {
        val signatureParameters = getNonReceiverParameters()
        methodDescriptor.receiver != receiverParameterInfo ||
        signatureParameters.size() != methodDescriptor.getParametersCount() ||
        signatureParameters.indices.any { i -> signatureParameters[i].getOldIndex() != i }
    }

    private var isPrimaryMethodUpdated: Boolean = false
    private var javaChangeInfos: List<JavaChangeInfo>? = null
    var originalToCurrentMethods: Map<PsiMethod, PsiMethod> = emptyMap()
        private set

    public fun getOldParameterIndex(oldParameterName: String): Int? = oldNameToParameterIndex[oldParameterName]

    override fun isParameterTypesChanged(): Boolean = true

    override fun isParameterNamesChanged(): Boolean = true

    override fun isParameterSetOrOrderChanged(): Boolean = isParameterSetOrOrderChangedLazy

    public fun getNewParametersCount(): Int = newParameters.size()

    override fun getNewParameters(): Array<JetParameterInfo> = newParameters.toTypedArray()

    fun getNonReceiverParametersCount(): Int = newParameters.size() - (if (receiverParameterInfo != null) 1 else 0)

    fun getNonReceiverParameters(): List<JetParameterInfo> {
        methodDescriptor.baseDeclaration.let { if (it is KtProperty || it is KtParameter) return emptyList() }
        return receiverParameterInfo?.let { receiver -> newParameters.filter { it != receiver } } ?: newParameters
    }

    public fun setNewParameter(index: Int, parameterInfo: JetParameterInfo) {
        newParameters.set(index, parameterInfo)
    }

    @JvmOverloads
    public fun addParameter(parameterInfo: JetParameterInfo, atIndex: Int = -1) {
        if (atIndex >= 0) {
            newParameters.add(atIndex, parameterInfo)
        }
        else {
            newParameters.add(parameterInfo)
        }
    }

    public fun removeParameter(index: Int) {
        val parameterInfo = newParameters.remove(index);
        if (parameterInfo == receiverParameterInfo) {
            receiverParameterInfo = null
        }
    }

    public fun clearParameters() {
        newParameters.clear()
        receiverParameterInfo = null
    }

    public fun hasParameter(parameterInfo: JetParameterInfo): Boolean =
            parameterInfo in newParameters

    override fun isGenerateDelegate(): Boolean = false

    override fun getNewName(): String = name

    fun setNewName(value: String) {
        name = value
    }

    override fun isNameChanged(): Boolean = name != methodDescriptor.getName()

    public fun isVisibilityChanged(): Boolean = newVisibility != methodDescriptor.getVisibility()

    override fun getMethod(): PsiElement {
        return methodDescriptor.getMethod()
    }

    override fun isReturnTypeChanged(): Boolean = newReturnTypeText != methodDescriptor.renderOriginalReturnType()

    fun isReceiverTypeChanged(): Boolean = receiverParameterInfo?.getTypeText() != methodDescriptor.renderOriginalReceiverType()

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    public var propagationTargetUsageInfos: List<UsageInfo> = ArrayList()
        private set

    public var primaryPropagationTargets: Collection<PsiElement> = emptyList()
        set(value: Collection<PsiElement>) {
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
                OverridingMethodsSearch.search(caller.getRepresentativeLightMethod() ?: continue).forEach { add(it) }
            }

            propagationTargetUsageInfos = result.toList()
        }

    init {
        this.primaryPropagationTargets = primaryPropagationTargets
    }

    public fun getNewSignature(inheritedCallable: JetCallableDefinitionUsage<PsiElement>): String {
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
                    buffer.append(it.currentTypeText).append('.')
                }
            }

            buffer.append(name)
        }

        buffer.append(getNewParametersSignature(inheritedCallable))

        if (newReturnType != null && !KotlinBuiltIns.isUnit(newReturnType) && kind == Kind.FUNCTION)
            buffer.append(": ").append(newReturnTypeText)

        return buffer.toString()
    }

    public fun isRefactoringTarget(inheritedCallableDescriptor: CallableDescriptor?): Boolean {
        return inheritedCallableDescriptor != null
               && getMethod() == DescriptorToSourceUtils.descriptorToDeclaration(inheritedCallableDescriptor)
    }

    public fun getNewParametersSignature(inheritedCallable: JetCallableDefinitionUsage<PsiElement>): String {
        return "(" + getNewParametersSignatureWithoutParentheses(inheritedCallable) + ")"
    }

    public fun getNewParametersSignatureWithoutParentheses(
            inheritedCallable: JetCallableDefinitionUsage<PsiElement>
    ): String {
        val signatureParameters = getNonReceiverParameters()

        val isLambda = inheritedCallable.getDeclaration() is KtFunctionLiteral
        if (isLambda && signatureParameters.size() == 1 && !signatureParameters.get(0).requiresExplicitType(inheritedCallable)) {
            return signatureParameters.get(0).getDeclarationSignature(0, inheritedCallable)
        }

        return signatureParameters.indices
                .map { i -> signatureParameters[i].getDeclarationSignature(i, inheritedCallable) }
                .joinToString(separator = ", ")
    }

    public fun renderReceiverType(inheritedCallable: JetCallableDefinitionUsage<PsiElement>): String? {
        val receiverTypeText = receiverParameterInfo?.currentTypeText ?: return null
        val typeSubstitutor = inheritedCallable.getOrCreateTypeSubstitutor() ?: return receiverTypeText
        val currentBaseFunction = inheritedCallable.getBaseFunction().getCurrentCallableDescriptor() ?: return receiverTypeText
        return currentBaseFunction.getExtensionReceiverParameter()!!.getType().renderTypeWithSubstitution(typeSubstitutor, receiverTypeText, false)
    }

    public fun renderReturnType(inheritedCallable: JetCallableDefinitionUsage<PsiElement>): String {
        val typeSubstitutor = inheritedCallable.getOrCreateTypeSubstitutor() ?: return newReturnTypeText
        val currentBaseFunction = inheritedCallable.getBaseFunction().getCurrentCallableDescriptor() ?: return newReturnTypeText
        return currentBaseFunction.getReturnType()!!.renderTypeWithSubstitution(typeSubstitutor, newReturnTypeText, false)
    }

    public fun primaryMethodUpdated() {
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

        val mandatoryParams = parameters.toArrayList()
        val defaultValues = ArrayList<KtExpression>()
        return psiMethods.map {
            JvmOverloadSignature(it, mandatoryParams.map(getPsi).toSet(), defaultValues.toSet()).apply {
                val param = mandatoryParams.removeLast { getDefaultValue(it) != null } ?: return@apply
                defaultValues.add(getDefaultValue(param)!!)
            }
        }
    }

    public fun getOrCreateJavaChangeInfos(): List<JavaChangeInfo>? {
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
                  && originalBaseFunctionDescriptor.hasJvmOverloadsAnnotation())) {
                return (originalPsiMethods zip currentPsiMethods).toMap()
            }

            if (originalPsiMethods.isEmpty() || currentPsiMethods.isEmpty()) return emptyMap()

            currentPsiMethods.singleOrNull()?.let { method -> return originalPsiMethods.keysToMap { method } }

            val currentSignatures = initCurrentSignatures(currentPsiMethods)
            return originalSignatures.toMap({ it.method }) { originalSignature ->
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
                VisibilityUtil.getVisibilityModifier(currentPsiMethod.getModifierList())
            else
                PsiModifier.PACKAGE_LOCAL
            val propagationTargets = primaryPropagationTargets.asSequence()
                    .map { it.getRepresentativeLightMethod() }
                    .filterNotNull()
                    .toSet()
            val javaChangeInfo = ChangeSignatureProcessor(
                    getMethod().getProject(),
                    originalPsiMethod,
                    false,
                    newVisibility,
                    newName,
                    CanonicalTypes.createTypeWrapper(newReturnType ?: PsiType.VOID),
                    newParameters,
                    arrayOf<ThrownExceptionInfo>(),
                    propagationTargets,
                    emptySet()
            ).getChangeInfo()
            javaChangeInfo.updateMethod(currentPsiMethod)

            return javaChangeInfo
        }

        fun getJavaParameterInfos(
                originalPsiMethod: PsiMethod,
                currentPsiMethod: PsiMethod,
                newParameterList: List<JetParameterInfo>
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
                    .map { pair ->
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
                            currentPsiMethod.getParameterList().getParameters()[indexInCurrentPsiMethod++].getType()
                        else
                            PsiType.VOID

                        val defaultValue = info.defaultValueForCall ?: info.defaultValueForParameter
                        ParameterInfoImpl(javaOldIndex, info.getName(), type, defaultValue?.getText() ?: "")
                    }
                    .filterNotNullTo(ArrayList())
        }

        fun createJavaChangeInfoForFunctionOrGetter(
                originalPsiMethod: PsiMethod,
                currentPsiMethod: PsiMethod,
                isGetter: Boolean
        ): JavaChangeInfo? {
            val newParameterList = receiverParameterInfo.singletonOrEmptyList() + getNonReceiverParameters()
            val newJavaParameters = getJavaParameterInfos(originalPsiMethod, currentPsiMethod, newParameterList).toTypedArray()
            val newName = if (isGetter) JvmAbi.getterName(getNewName()) else getNewName()
            return createJavaChangeInfo(originalPsiMethod, currentPsiMethod, newName, currentPsiMethod.getReturnType(), newJavaParameters)
        }

        fun createJavaChangeInfoForSetter(originalPsiMethod: PsiMethod, currentPsiMethod: PsiMethod): JavaChangeInfo? {
            val newJavaParameters = getJavaParameterInfos(originalPsiMethod, currentPsiMethod, receiverParameterInfo.singletonOrEmptyList())
            val oldIndex = if (methodDescriptor.receiver != null) 1 else 0
            if (isPrimaryMethodUpdated) {
                val newIndex = if (receiverParameterInfo != null) 1 else 0
                val setterParameter = currentPsiMethod.getParameterList().getParameters()[newIndex]
                newJavaParameters.add(ParameterInfoImpl(oldIndex, setterParameter.getName(), setterParameter.getType()))
            }
            else {
                newJavaParameters.add(ParameterInfoImpl(oldIndex, "receiver", PsiType.VOID))
            }

            val newName = JvmAbi.setterName(getNewName())
            return createJavaChangeInfo(originalPsiMethod, currentPsiMethod, newName, PsiType.VOID, newJavaParameters.toTypedArray())
        }

        if (ProjectStructureUtil.isJsKotlinModule(getMethod().getContainingFile() as KtFile)) return null

        if (javaChangeInfos == null) {
            val method = getMethod()
            originalToCurrentMethods = matchOriginalAndCurrentMethods(method.toLightMethods())
            javaChangeInfos = originalToCurrentMethods.entries.map {
                val (originalPsiMethod, currentPsiMethod) = it

                when (method) {
                    is KtFunction, is KtClassOrObject ->
                        createJavaChangeInfoForFunctionOrGetter(originalPsiMethod, currentPsiMethod, false)
                    is KtProperty, is KtParameter -> {
                        val accessorName = originalPsiMethod.getName()
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
            }.filterNotNull()
        }

        return javaChangeInfos
    }
}

public val JetChangeInfo.originalBaseFunctionDescriptor: CallableDescriptor
    get() = methodDescriptor.baseDescriptor

public val JetChangeInfo.kind: Kind get() = methodDescriptor.kind

public val JetChangeInfo.oldName: String?
    get() = (methodDescriptor.getMethod() as? KtFunction)?.getName()

public fun JetChangeInfo.getAffectedCallables(): Collection<UsageInfo> = methodDescriptor.affectedCallables + propagationTargetUsageInfos

public fun ChangeInfo.toJetChangeInfo(originalChangeSignatureDescriptor: JetMethodDescriptor): JetChangeInfo {
    val method = getMethod() as PsiMethod

    val functionDescriptor = method.getJavaMethodDescriptor()!!
    val parameterDescriptors = functionDescriptor.getValueParameters()

    //noinspection ConstantConditions
    val originalParameterDescriptors = originalChangeSignatureDescriptor.baseDescriptor.getValueParameters()

    val newParameters = getNewParameters().withIndex().map { pair ->
        val (i, info) = pair
        val oldIndex = info.getOldIndex()
        val currentType = parameterDescriptors[i].getType()

        val defaultValueText = info.getDefaultValue()
        val defaultValueExpr =
                when {
                    info is KotlinAwareJavaParameterInfoImpl -> info.kotlinDefaultValue
                    getLanguage().`is`(JavaLanguage.INSTANCE) && !defaultValueText.isNullOrEmpty() -> {
                        PsiElementFactory.SERVICE.getInstance(method.getProject())
                                .createExpressionFromText(defaultValueText!!, null)
                                .j2k()
                    }
                    else -> null
                }

        with(JetParameterInfo(callableDescriptor = functionDescriptor,
                              originalIndex = oldIndex,
                              name = info.getName(),
                              type = if (oldIndex >= 0) originalParameterDescriptors[oldIndex].getType() else currentType,
                              defaultValueForCall = defaultValueExpr)) {
            currentTypeText = IdeDescriptorRenderers.SOURCE_CODE_FOR_TYPE_ARGUMENTS.renderType(currentType)
            this
        }
    }

    val returnType = functionDescriptor.getReturnType()
    val returnTypeText = if (returnType != null) IdeDescriptorRenderers.SOURCE_CODE.renderType(returnType) else ""

    return JetChangeInfo(originalChangeSignatureDescriptor,
                         getNewName(),
                         returnType,
                         returnTypeText,
                         functionDescriptor.getVisibility(),
                         newParameters,
                         null,
                         method)
}
