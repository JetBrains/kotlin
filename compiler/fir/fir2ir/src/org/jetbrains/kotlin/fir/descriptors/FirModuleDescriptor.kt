/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class FirModuleDescriptor private constructor(
    val session: FirSession,
    val moduleData: FirModuleData,
    override val builtIns: KotlinBuiltIns
) : ModuleDescriptor {
    companion object {
        fun createSourceModuleDescriptor(session: FirSession, builtIns: KotlinBuiltIns): FirModuleDescriptor {
            require(session.kind == FirSession.Kind.Source)
            return FirModuleDescriptor(session, session.moduleData, builtIns)
        }

        fun createDependencyModuleDescriptor(moduleData: FirModuleData, builtIns: KotlinBuiltIns): FirModuleDescriptor {
            // We may create dependency module descriptor for java sources, which have source session and source module data
            return FirModuleDescriptor(moduleData.session, moduleData, builtIns)
        }
    }

    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean {
        if (targetModule !is FirModuleDescriptor) return false
        return when (targetModule.moduleData) {
            this.moduleData,
            in moduleData.friendDependencies,
            in moduleData.dependsOnDependencies -> true
            else -> false
        }
    }

    override val platform: TargetPlatform
        get() = moduleData.platform

    override fun getPackage(fqName: FqName): PackageViewDescriptor {
        val symbolProvider = session.symbolProvider
        if (symbolProvider.getPackage(fqName) != null) {
            return FirPackageViewDescriptor(fqName, this)
        }
        error("Module $moduleData doesn't contain package $fqName")
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        shouldNotBeCalled()
    }

    override var allDependencyModules: List<ModuleDescriptor> = emptyList()

    override val expectedByModules: List<ModuleDescriptor>
        get() = shouldNotBeCalled()
    override val allExpectedByModules: Set<ModuleDescriptor>
        get() = shouldNotBeCalled()

    override fun <T> getCapability(capability: ModuleCapability<T>): T? {
        return null
    }

    override val isValid: Boolean
        get() = true

    override fun assertValid() {

    }

    override fun getOriginal(): DeclarationDescriptor {
        return this
    }

    override fun getName(): Name {
        return moduleData.name
    }

    override val stableName: Name
        get() = name

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        shouldNotBeCalled()
    }

    override val annotations: Annotations
        get() = Annotations.EMPTY
}
