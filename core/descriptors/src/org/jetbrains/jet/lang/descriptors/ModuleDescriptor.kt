/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors

import org.jetbrains.jet.lang.PlatformToKotlinClassMap
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.descriptors.impl.PackageViewDescriptorImpl

public trait ModuleDescriptor : DeclarationDescriptor {
    override fun getContainingDeclaration(): DeclarationDescriptor? = null

    public fun getPackageFragmentProvider(): PackageFragmentProvider

    public fun getPackage(fqName: FqName): PackageViewDescriptor? {
        val fragments = getPackageFragmentProvider().getPackageFragments(fqName)
        return if (!fragments.isEmpty()) PackageViewDescriptorImpl(this, fqName, fragments) else null
    }

    public val defaultImports: List<ImportPath>

    public val platformToKotlinClassMap: PlatformToKotlinClassMap

    override fun substitute(substitutor: TypeSubstitutor): ModuleDescriptor {
        return this
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitModuleDeclaration(this, data)
    }
}
