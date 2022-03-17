/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

object ErrorModuleDescriptor : ModuleDescriptor {
    override val stableName: Name = Name.special(ErrorEntity.ERROR_MODULE.debugText)
    override val platform: TargetPlatform? = null
    override val allDependencyModules: List<ModuleDescriptor> = emptyList()
    override val expectedByModules: List<ModuleDescriptor> = emptyList()
    override val allExpectedByModules: Set<ModuleDescriptor> = emptySet()
    override val annotations: Annotations get() = Annotations.EMPTY
    override val builtIns: KotlinBuiltIns = DefaultBuiltIns.Instance
    override val isValid: Boolean = false

    override fun <T> getCapability(capability: ModuleCapability<T>): T? = null
    override fun getSubPackagesOf(fqName: FqName, nameFilter: Function1<Name, Boolean>): Collection<FqName> = emptyList()
    override fun getName(): Name = stableName
    override fun getPackage(fqName: FqName): PackageViewDescriptor = throw IllegalStateException("Should not be called!")
    override fun getOriginal(): DeclarationDescriptor = this
    override fun getContainingDeclaration(): DeclarationDescriptor? = null
    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean = false
    override fun assertValid() = throw InvalidModuleException("ERROR_MODULE is not a valid module")
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? = null
    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {}
}