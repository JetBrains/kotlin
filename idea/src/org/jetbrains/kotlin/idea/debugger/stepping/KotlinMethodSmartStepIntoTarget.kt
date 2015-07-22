package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.psi.PsiElement
import com.intellij.util.Range
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import javax.swing.Icon

public class KotlinMethodSmartStepTarget(
        val resolvedElement: JetElement,
        label: String,
        highlightElement: PsiElement,
        lines: Range<Int>
): SmartStepTarget(label, highlightElement, false, lines) {
    override fun getIcon(): Icon? {
        return when {
            resolvedElement is JetNamedFunction && resolvedElement.getReceiverTypeReference() != null -> JetIcons.EXTENSION_FUNCTION
            else -> JetIcons.FUNCTION
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

        return resolvedElement == other.resolvedElement
    }

    override fun hashCode() = resolvedElement.hashCode()
}