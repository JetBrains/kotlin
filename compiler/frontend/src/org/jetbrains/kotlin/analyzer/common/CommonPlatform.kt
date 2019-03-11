/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.types.DynamicTypesAllowed
import org.jetbrains.kotlin.types.DynamicTypesSettings

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

object CommonPlatformCompilerServices : PlatformDependentCompilerServices() {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {}

    override val platformConfigurator: PlatformConfigurator = CommonPlatformConfigurator

    override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.AFTER_SDK
}

class CompositePlatformCompilerServices(val services: List<PlatformDependentCompilerServices>) : PlatformDependentCompilerServices() {
    override val platformConfigurator: PlatformConfigurator
        get() = CompositePlatformConigurator(services.map { it.platformConfigurator as PlatformConfiguratorBase })

    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        val intersectionOfDefaultImports = services.map { service ->
            mutableListOf<ImportPath>()
                .apply { service.computePlatformSpecificDefaultImports(storageManager, this) }
                .toSet()
        }.reduce { first, second -> first.intersect(second) }

        result.addAll(intersectionOfDefaultImports)
    }
}

class CompositePlatformConigurator(private val configurators: List<PlatformConfiguratorBase>) : PlatformConfiguratorBase(
    dynamicTypesSettings = configurators.map { it.dynamicTypesSettings }.merge(),
    additionalDeclarationCheckers = configurators.flatMap { it.additionalDeclarationCheckers },
    additionalCallCheckers = configurators.flatMap { it.additionalCallCheckers },
    additionalTypeCheckers = configurators.flatMap { it.additionalTypeCheckers },
    additionalClassifierUsageCheckers = configurators.flatMap { it.additionalClassifierUsageCheckers },
    additionalAnnotationCheckers = configurators.flatMap { it.additionalAnnotationCheckers },
    identifierChecker = configurators.map { it.identifierChecker }.merge(),
    overloadFilter = configurators.map { it.overloadFilter }.merge(),
    platformToKotlinClassMap = configurators.map { it.platformToKotlinClassMap }.merge(),
    delegationFilter = configurators.map { it.delegationFilter }.merge(),
    overridesBackwardCompatibilityHelper = configurators.map { it.overridesBackwardCompatibilityHelper }.merge(),
    declarationReturnTypeSanitizer = configurators.map { it.declarationReturnTypeSanitizer }.merge()
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        // TODO: hack
        configurators.first().configureModuleComponents(container)
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        configurators.forEach { it.configureModuleDependentCheckers(container) }
    }
}


// TODO: hacks below
private fun List<DynamicTypesSettings>.merge(): DynamicTypesSettings =
    if (any { it.dynamicTypesAllowed }) DynamicTypesAllowed() else DynamicTypesSettings()

private fun List<OverloadFilter>.merge(): OverloadFilter = first()
private fun List<IdentifierChecker>.merge(): IdentifierChecker = first()
private fun List<PlatformToKotlinClassMap>.merge(): PlatformToKotlinClassMap = first()
private fun List<DelegationFilter>.merge(): DelegationFilter = first()
private fun List<OverridesBackwardCompatibilityHelper>.merge(): OverridesBackwardCompatibilityHelper = first()
private fun List<DeclarationReturnTypeSanitizer>.merge(): DeclarationReturnTypeSanitizer = first()