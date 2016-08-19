/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.TypeSubstitutor

interface IrBuiltinsPackageFragmentDescriptor : PackageFragmentDescriptor

class IrBuiltinsPackageFragmentDescriptorImpl(val containingModule: ModuleDescriptor) : IrBuiltinsPackageFragmentDescriptor {
    override val fqName: FqName get() = FqName("kotlin.internal.ir")
    override fun getName(): Name = Name.identifier("ir")

    override fun getContainingDeclaration(): ModuleDescriptor = containingModule

    override fun getMemberScope(): MemberScope = MemberScope.Empty

    override fun getOriginal(): DeclarationDescriptorWithSource = this
    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override val annotations: Annotations = Annotations.EMPTY
    override fun substitute(substitutor: TypeSubstitutor): DeclarationDescriptor = throw UnsupportedOperationException()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitPackageFragmentDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitPackageFragmentDescriptor(this, null)
    }
}


