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

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*

interface FileClassDescriptor : ClassDescriptor

class FileClassDescriptorImpl(
        private val nameImpl: Name,
        private val containingDeclarationImpl: PackageFragmentDescriptor,
        private val sourceElement: SourceElement
) : FileClassDescriptor {
    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null
    override fun getConstructors(): Collection<ConstructorDescriptor> = emptyList()
    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclarationImpl
    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getDefaultType(): SimpleType = ErrorUtils.createErrorType("File class type for $nameImpl")
    override fun getKind(): ClassKind = ClassKind.CLASS
    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope = error("File class has no member scope")
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope = error("File class has no member scope")
    override fun getModality(): Modality = Modality.FINAL
    override fun getOriginal(): ClassDescriptor = this
    override fun getName(): Name = nameImpl
    override fun getStaticScope(): MemberScope = error("File class has no static scope")
    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor = error("File class has no instances")
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = error("File class has no inner classes scope")
    override fun getUnsubstitutedMemberScope(): MemberScope = error("File class has no member scope")
    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = null
    override fun getVisibility(): Visibility = Visibilities.PUBLIC
    override fun isCompanionObject(): Boolean = false
    override fun isData(): Boolean = false
    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor = error("File class can't be substituted")
    override fun getSource(): SourceElement = sourceElement
    override fun getTypeConstructor(): TypeConstructor = error("File class can't be used in types")
    override fun isInner(): Boolean = false

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitClassDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }

    override val annotations: Annotations
        get() = TODO("not implemented")

    override fun toString(): String =
            "IrFileClassDescriptor($fqNameUnsafe)"
}