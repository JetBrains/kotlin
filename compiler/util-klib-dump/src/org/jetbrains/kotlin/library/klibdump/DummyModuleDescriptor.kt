/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.klibdump

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

internal class DummyModuleDescriptor(private val moduleName: Name, override val stableName: Name? = null) : ModuleDescriptor {
    override fun getOriginal(): DeclarationDescriptor = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? = null

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {}

    override val builtIns: KotlinBuiltIns
        get() = TODO("Not yet implemented")

    override val platform: TargetPlatform?
        get() = null

    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean = false

    override fun getPackage(fqName: FqName): PackageViewDescriptor {
        throw UnsupportedOperationException()
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> = emptyList()

    override val allDependencyModules: List<ModuleDescriptor>
        get() = emptyList()

    override val expectedByModules: List<ModuleDescriptor>
        get() = emptyList()

    override val allExpectedByModules: Set<ModuleDescriptor>
        get() = emptySet()

    override fun <T> getCapability(capability: ModuleCapability<T>): T? = null

    override val isValid: Boolean
        get() = true // TODO: Is this right?

    override fun assertValid() {
        require(isValid)
    }

    override fun getName(): Name = moduleName

    override val annotations: Annotations
        get() = Annotations.EMPTY
}
