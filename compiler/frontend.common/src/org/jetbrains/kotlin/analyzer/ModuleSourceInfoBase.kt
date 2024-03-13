/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.name.FqName

interface PlatformAnalysisParameters {
    object Empty : PlatformAnalysisParameters
}

interface PackageOracle {
    fun packageExists(fqName: FqName): Boolean

    object Optimistic : PackageOracle {
        override fun packageExists(fqName: FqName): Boolean = true
    }
}

interface PackageOracleFactory {
    fun createOracle(moduleInfo: ModuleInfo): PackageOracle

    object OptimisticFactory : PackageOracleFactory {
        override fun createOracle(moduleInfo: ModuleInfo) = PackageOracle.Optimistic
    }
}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(moduleInfo: ModuleInfo)

    companion object {
        @Suppress("DEPRECATION") // KT-68390
        fun getInstance(project: Project): ResolverForModuleComputationTracker? =
            project.getComponent(ResolverForModuleComputationTracker::class.java) ?: null
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> ModuleInfo.getCapability(capability: ModuleCapability<T>) = capabilities[capability] as? T

interface CombinedModuleInfo : ModuleInfo {
    val containedModules: List<ModuleInfo>
    val platformModule: ModuleInfo
}

/**
 * Special-purpose module info that allows implementors to provide different behavior compared to the [originalModule]'s.
 * E.g. may be used to resolve common code as if it were target-specific, or to change the dependencies visible to the code.
 *
 * Resolvers should accept a derived module info, iff the [originalModule] is accepted.
 */
interface DerivedModuleInfo : ModuleInfo {
    val originalModule: ModuleInfo
}

fun ModuleInfo.flatten(): List<ModuleInfo> = when (this) {
    is CombinedModuleInfo -> listOf(this) + containedModules
    else -> listOf(this)
}

fun ModuleInfo.unwrapPlatform(): ModuleInfo = if (this is CombinedModuleInfo) platformModule else this

interface LibraryModuleSourceInfoBase : ModuleInfo
interface NonSourceModuleInfoBase : ModuleInfo
interface ModuleSourceInfoBase : ModuleInfo
interface SdkInfoBase : ModuleInfo

