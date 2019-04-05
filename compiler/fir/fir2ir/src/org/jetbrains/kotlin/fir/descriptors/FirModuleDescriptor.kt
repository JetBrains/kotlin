/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.descriptors

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirModuleDescriptor(val session: FirSession) : ModuleDescriptor {
    override val builtIns: KotlinBuiltIns
        get() = DefaultBuiltIns.Instance

    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean {
        return false
    }

    override fun getPackage(fqName: FqName): PackageViewDescriptor {
        val symbolProvider = FirSymbolProvider.getInstance(session)
        if (symbolProvider.getPackage(fqName) != null) {
            return FirPackageViewDescriptor(fqName, this)
        }
        TODO("Missing package reporting")
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        TODO("not implemented")
    }

    override val allDependencyModules: List<ModuleDescriptor>
        get() = TODO("not implemented")
    override val expectedByModules: List<ModuleDescriptor>
        get() = TODO("not implemented")

    override fun <T> getCapability(capability: ModuleDescriptor.Capability<T>): T? {
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
        return Name.identifier("module for FIR session")
    }

    override val stableName: Name?
        get() = name

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        TODO("not implemented")
    }

    override val annotations: Annotations
        get() = TODO("not implemented")

}