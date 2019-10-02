package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ModuleStructureOracle
import org.jetbrains.kotlin.resolve.PlatformConfiguratorBase
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.inline.ReasonableInlineRule
import org.jetbrains.kotlin.resolve.jvm.checkers.SuperCallWithDefaultArgumentsChecker

object KonanPlatformConfigurator : PlatformConfiguratorBase(
    additionalDeclarationCheckers = listOf(ExpectedActualDeclarationChecker(ModuleStructureOracle.SingleModule, emptyList())),
    additionalCallCheckers = listOf(SuperCallWithDefaultArgumentsChecker())
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(NativeInliningRule)
    }
}

object NativeInliningRule : ReasonableInlineRule {
    override fun isInlineReasonable(
            descriptor: CallableMemberDescriptor,
            declaration: KtCallableDeclaration,
            context: BindingContext
    ): Boolean = true
}