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

package org.jetbrains.kotlin.resolve.jvm.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

public enum class MemberKind { FIELD METHOD }

public data class RawSignature(public val name: String, public val desc: String, public val kind: MemberKind)

public enum class JvmDeclarationOriginKind {
    OTHER
    PACKAGE_FACADE
    PACKAGE_PART
    TRAIT_IMPL
    DELEGATION_TO_TRAIT_IMPL
    DELEGATION
    BRIDGE
    SYNTHETIC // this means that there's no proper descriptor for this jvm declaration
}

public class JvmDeclarationOrigin(
        public val originKind: JvmDeclarationOriginKind,
        public val element: PsiElement?,
        public val descriptor: DeclarationDescriptor?
) {
    default object {
        public val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin(OTHER, null, null)
    }
}

public fun OtherOrigin(element: PsiElement?, descriptor: DeclarationDescriptor?): JvmDeclarationOrigin =
        if (element == null && descriptor == null)
            JvmDeclarationOrigin.NO_ORIGIN
        else JvmDeclarationOrigin(OTHER, element, descriptor)

public fun OtherOrigin(element: PsiElement): JvmDeclarationOrigin = OtherOrigin(element, null)

public fun OtherOrigin(descriptor: DeclarationDescriptor): JvmDeclarationOrigin = OtherOrigin(null, descriptor)

public fun Bridge(descriptor: DeclarationDescriptor, element: PsiElement? = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)): JvmDeclarationOrigin =
        JvmDeclarationOrigin(BRIDGE, element, descriptor)

public fun PackageFacade(descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(PACKAGE_FACADE, null, descriptor)
public fun PackagePart(file: JetFile, descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(PACKAGE_PART, file, descriptor)

public fun TraitImpl(element: JetClassOrObject, descriptor: ClassDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(TRAIT_IMPL, element, descriptor)
public fun DelegationToTraitImpl(element: PsiElement?, descriptor: FunctionDescriptor): JvmDeclarationOrigin =
        JvmDeclarationOrigin(DELEGATION_TO_TRAIT_IMPL, element, descriptor)

public fun Delegation(element: PsiElement?, descriptor: FunctionDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(DELEGATION, element, descriptor)

public fun Synthetic(element: PsiElement?, descriptor: CallableMemberDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(SYNTHETIC, element, descriptor)