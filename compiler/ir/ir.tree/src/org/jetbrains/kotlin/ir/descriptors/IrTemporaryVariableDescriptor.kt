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
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

interface IrTemporaryVariableDescriptor : VariableDescriptor

class IrTemporaryVariableDescriptorImpl(
        containingDeclaration: DeclarationDescriptor,
        name: Name,
        outType: KotlinType
): VariableDescriptorImpl(containingDeclaration, Annotations.EMPTY, name, outType, SourceElement.NO_SOURCE),
        IrTemporaryVariableDescriptor
{
    override fun getCompileTimeInitializer(): ConstantValue<*>? = null

    override fun getVisibility(): Visibility = Visibilities.LOCAL

    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor {
        throw UnsupportedOperationException("Temporary variable descriptor shouldn't be substituted (so far): $this")
    }

    override fun isVar(): Boolean = false

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
            visitor.visitVariableDescriptor(this, data)
}