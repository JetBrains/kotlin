package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.suggested.*
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Parameter
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.project.forcedModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasBody
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError

class KotlinSuggestedRefactoringAvailability(refactoringSupport: SuggestedRefactoringSupport) :
    SuggestedRefactoringAvailability(refactoringSupport)
{
    override fun refineSignaturesWithResolve(state: SuggestedRefactoringState): SuggestedRefactoringState {
        val newDeclaration = state.declaration as? KtCallableDeclaration ?: return state
        val oldDeclaration = state.createRestoredDeclarationCopy(refactoringSupport) as? KtCallableDeclaration ?: return state
        oldDeclaration.containingKtFile.forcedModuleInfo = newDeclaration.getModuleInfo()

        val descriptorWithOldSignature = oldDeclaration.resolveToDescriptorIfAny() as CallableDescriptor
        val descriptorWithNewSignature = newDeclaration.resolveToDescriptorIfAny() as CallableDescriptor

        val oldSignature = state.oldSignature
        val newSignature = state.newSignature

        val (oldReturnType, newReturnType) = refineType(
            oldSignature.type,
            newSignature.type,
            descriptorWithOldSignature.returnType,
            descriptorWithNewSignature.returnType
        )

        val improvedOldParameterTypesById = mutableMapOf<Any, String>()
        val improvedNewParameterTypesById = mutableMapOf<Any, String>()
        for (oldParameter in oldSignature.parameters) {
            val id = oldParameter.id
            val newParameter = newSignature.parameterById(id) ?: continue
            val oldIndex = oldSignature.parameterIndex(oldParameter)
            val newIndex = newSignature.parameterIndex(newParameter)
            val (oldType, newType) = refineType(
                oldParameter.type,
                newParameter.type,
                descriptorWithOldSignature.valueParameters[oldIndex].type,
                descriptorWithNewSignature.valueParameters[newIndex].type
            )
            // oldType and newType may not be null because arguments of refineType call were all non-null
            improvedOldParameterTypesById[id] = oldType!!
            improvedNewParameterTypesById[id] = newType!!
        }
        val oldParameters = oldSignature.parameters.map { it.copy(type = improvedOldParameterTypesById[it.id] ?: it.type) }
        val newParameters = newSignature.parameters.map { it.copy(type = improvedNewParameterTypesById[it.id] ?: it.type) }

        val oldAdditionalData = oldSignature.additionalData as KotlinSignatureAdditionalData?
        val newAdditionalData = newSignature.additionalData as KotlinSignatureAdditionalData?
        val (oldReceiverType, newReceiverType) = refineType(
            oldAdditionalData?.receiverType,
            newAdditionalData?.receiverType,
            descriptorWithOldSignature.extensionReceiverParameter?.type,
            descriptorWithNewSignature.extensionReceiverParameter?.type
        )

        return state.copy(
            oldSignature = Signature.create(
                oldSignature.name,
                oldReturnType,
                oldParameters,
                oldAdditionalData?.copy(receiverType = oldReceiverType)
            )!!,
            newSignature = Signature.create(
                newSignature.name,
                newReturnType,
                newParameters,
                newAdditionalData?.copy(receiverType = newReceiverType)
            )!!
        )
    }

    private fun refineType(
        oldTypeInCode: String?,
        newTypeInCode: String?,
        oldTypeResolved: KotlinType?,
        newTypeResolved: KotlinType?
    ): Pair<String?, String?> {
        if (oldTypeResolved?.isError == true || newTypeResolved?.isError == true) {
            return oldTypeInCode to newTypeInCode
        }

        val oldTypeFQ = oldTypeResolved?.fqText()
        val newTypeFQ = newTypeResolved?.fqText()

        if (oldTypeInCode != newTypeInCode) {
            if (oldTypeFQ == newTypeFQ) {
                return newTypeInCode to newTypeInCode
            }
        } else {
            if (oldTypeFQ != newTypeFQ) {
                return oldTypeFQ to newTypeFQ
            }
        }

        return oldTypeInCode to newTypeInCode
    }

    private fun KotlinType.fqText() = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(this)

    override fun detectAvailableRefactoring(state: SuggestedRefactoringState): SuggestedRefactoringData? {
        val declaration = state.declaration
        if (declaration !is KtCallableDeclaration || KotlinSuggestedRefactoringSupport.isOnlyRenameSupported(declaration)) {
            return SuggestedRenameData(declaration as PsiNamedElement, state.oldSignature.name)
        }

        val oldSignature = state.oldSignature
        val newSignature = state.newSignature
        val updateUsagesData = SuggestedChangeSignatureData.create(state, USAGES)

        if (hasParameterAddedRemovedOrReordered(oldSignature, newSignature)) return updateUsagesData

        // for non-virtual function we can add or remove receiver for usages but not change its type
        if ((oldSignature.receiverType == null) != (newSignature.receiverType == null)) return updateUsagesData

        val (nameChanges, renameData) = nameChanges(oldSignature, newSignature, declaration, declaration.valueParameters)

        if (hasTypeChanges(oldSignature, newSignature)) {
            return if (nameChanges > 0)
                updateUsagesData
            else
                declaration.overridesName()?.let { updateUsagesData.copy(nameOfStuffToUpdate = it) }
        }

        return when {
            renameData != null -> renameData
            nameChanges > 0 -> updateUsagesData
            else -> null
        }
    }

    private fun KtCallableDeclaration.overridesName(): String? {
        return when {
            hasModifier(KtTokens.ABSTRACT_KEYWORD) -> IMPLEMENTATIONS
            hasModifier(KtTokens.OPEN_KEYWORD) -> OVERRIDES
            containingClassOrObject?.isInterfaceClass() == true -> if (hasBody()) OVERRIDES else IMPLEMENTATIONS
            hasModifier(KtTokens.EXPECT_KEYWORD) -> "actual declarations"
            else -> null
        }
    }

    override fun hasTypeChanges(oldSignature: Signature, newSignature: Signature): Boolean {
        return super.hasTypeChanges(oldSignature, newSignature) || oldSignature.receiverType != newSignature.receiverType
    }

    override fun hasParameterTypeChanges(oldParam: Parameter, newParam: Parameter): Boolean {
        return super.hasParameterTypeChanges(oldParam, newParam) || oldParam.modifiers != newParam.modifiers
    }
}
