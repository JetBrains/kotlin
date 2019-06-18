package androidx.compose.plugins.kotlin.frames.analysis

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import androidx.compose.plugins.kotlin.ComposeUtils
import androidx.compose.plugins.kotlin.analysis.ComposeDefaultErrorMessages
import androidx.compose.plugins.kotlin.analysis.ComposeErrors
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class FrameModelChecker : DeclarationChecker, StorageComponentContainerContributor {

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (platform != JvmPlatform) return
        container.useInstance(FrameModelChecker())
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is ClassDescriptor) {
            if (!descriptor.isModelClass) return

            if (declaration.hasModifier(KtTokens.OPEN_KEYWORD) ||
                declaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                val element = (declaration as? KtClass)?.nameIdentifier ?: declaration
                context.trace.reportFromPlugin(
                    ComposeErrors.OPEN_MODEL.on(element),
                    ComposeDefaultErrorMessages
                )
            }
        }
    }
}

private val MODEL_FQNAME = ComposeUtils.composeFqName("Model")
val DeclarationDescriptor.isModelClass: Boolean get() = annotations.hasAnnotation(MODEL_FQNAME)
