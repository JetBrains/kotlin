package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.psi.PsiElement
import com.intellij.util.Range
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import javax.swing.Icon

class KotlinMethodSmartStepTarget(
        val descriptor: CallableMemberDescriptor,
        label: String,
        highlightElement: PsiElement,
        lines: Range<Int>
): SmartStepTarget(label, highlightElement, false, lines) {
    override fun getIcon(): Icon? {
        return when {
            descriptor.isExtension -> KotlinIcons.EXTENSION_FUNCTION
            else -> KotlinIcons.FUNCTION
        }
    }

    companion object {
        private val renderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.withOptions {
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            withoutReturnType = true
            renderAccessors = true
            startFromName = true
            modifiers = emptySet()
        }

        fun calcLabel(descriptor: DeclarationDescriptor): String {
            return renderer.render(descriptor)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other == null || other !is KotlinMethodSmartStepTarget) return false

        return descriptor == other.descriptor
    }

    override fun hashCode() = descriptor.hashCode()
}