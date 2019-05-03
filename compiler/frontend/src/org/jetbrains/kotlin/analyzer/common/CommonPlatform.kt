/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer.common

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.components.SamConversionTransformer
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.DynamicTypesSettings

object CommonPlatform : TargetPlatform("Default") {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {}

    override val multiTargetPlatform: MultiTargetPlatform
        get() = MultiTargetPlatform.Common

    override val platformConfigurator: PlatformConfigurator = CommonPlatformConfigurator

    override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.AFTER_SDK
}

private object CommonPlatformConfigurator : PlatformConfiguratorBase(
    DynamicTypesSettings(), listOf(), listOf(), listOf(), listOf(), listOf(),
    IdentifierChecker.Default, OverloadFilter.Default, PlatformToKotlinClassMap.EMPTY, DelegationFilter.Default,
    OverridesBackwardCompatibilityHelper.Default,
    DeclarationReturnTypeSanitizer.Default
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(SyntheticScopes.Empty)
        container.useInstance(SamConversionTransformer.Empty)
        container.useInstance(TypeSpecificityComparator.NONE)
    }
}

