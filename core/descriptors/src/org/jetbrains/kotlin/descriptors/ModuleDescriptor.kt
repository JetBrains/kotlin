/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

interface ModuleDescriptor : DeclarationDescriptor {
    override fun getContainingDeclaration(): DeclarationDescriptor? = null

    val builtIns: KotlinBuiltIns

    /**
     * Stable name of *Kotlin* module. Can be used for ABI (e.g. for mangling of declarations)
     */
    val stableName: Name?

    // NB: this field should actually be non-null, but making it so implies a LOT of work, so we postpone it for a moment
    // TODO: make it non-null
    val platform: TargetPlatform?

    fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
        return visitor.visitModuleDeclaration(this, data)
    }

    fun getPackage(fqName: FqName): PackageViewDescriptor

    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>

    /**
     * @return dependency modules in the same order in which this module depends on them. Does not include `this`
     */
    val allDependencyModules: List<ModuleDescriptor>

    val expectedByModules: List<ModuleDescriptor>

    val allExpectedByModules: Set<ModuleDescriptor>

    fun <T> getCapability(capability: ModuleCapability<T>): T?

    class Capability<T>(val name: String) {
        override fun toString() = name
    }

    val isValid: Boolean

    fun assertValid()
}
