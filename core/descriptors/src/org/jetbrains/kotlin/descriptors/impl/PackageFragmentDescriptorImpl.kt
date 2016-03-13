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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.TypeSubstitutor

abstract class PackageFragmentDescriptorImpl(
        module: ModuleDescriptor,
        final override val fqName: FqName
) : DeclarationDescriptorNonRootImpl(module, Annotations.EMPTY, fqName.shortNameOrSpecial(), SourceElement.NO_SOURCE),
        PackageFragmentDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): DeclarationDescriptor? = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
            visitor.visitPackageFragmentDescriptor(this, data)

    override fun getContainingDeclaration(): ModuleDescriptor {
        return super<DeclarationDescriptorNonRootImpl>.getContainingDeclaration() as ModuleDescriptor
    }

    override fun getSource(): SourceElement {
        return SourceElement.NO_SOURCE
    }

    override fun toString(): String = "package $fqName"
}
