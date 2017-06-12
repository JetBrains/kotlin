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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPureElement
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.*

enum class MemberKind { FIELD, METHOD }

data class RawSignature(val name: String, val desc: String, val kind: MemberKind)

enum class JvmDeclarationOriginKind {
    OTHER,
    PACKAGE_PART,
    INTERFACE_DEFAULT_IMPL,
    CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL,
    DEFAULT_IMPL_DELEGATION_TO_SUPERINTERFACE_DEFAULT_IMPL,
    DELEGATION,
    SAM_DELEGATION,
    BRIDGE,
    MULTIFILE_CLASS,
    MULTIFILE_CLASS_PART,
    SYNTHETIC, // this means that there's no proper descriptor for this jvm declaration,
    COLLECTION_STUB,
    AUGMENTED_BUILTIN_API
}

class JvmDeclarationOrigin(
        val originKind: JvmDeclarationOriginKind,
        val element: PsiElement?,
        val descriptor: DeclarationDescriptor?
) {
    companion object {
        @JvmField val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin(OTHER, null, null)
    }
}

fun OtherOrigin(element: PsiElement?, descriptor: DeclarationDescriptor?): JvmDeclarationOrigin =
        if (element == null && descriptor == null)
            JvmDeclarationOrigin.NO_ORIGIN
        else JvmDeclarationOrigin(OTHER, element, descriptor)

fun OtherOrigin(element: KtPureElement?, descriptor: DeclarationDescriptor?): JvmDeclarationOrigin =
        OtherOrigin(element?.psiOrParent as PsiElement?, descriptor)

fun OtherOrigin(element: KtElement, descriptor: DeclarationDescriptor?): JvmDeclarationOrigin =
        OtherOrigin(element as PsiElement, descriptor)

fun OtherOrigin(element: PsiElement): JvmDeclarationOrigin = OtherOrigin(element, null)

fun OtherOrigin(element: KtPureElement): JvmDeclarationOrigin = OtherOrigin(element, null)

fun OtherOrigin(element: KtElement): JvmDeclarationOrigin = OtherOrigin(element, null)

fun OtherOrigin(descriptor: DeclarationDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(OTHER, null, descriptor)

fun Bridge(descriptor: DeclarationDescriptor, element: PsiElement? = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)): JvmDeclarationOrigin =
        JvmDeclarationOrigin(BRIDGE, element, descriptor)

fun PackagePart(file: KtFile, descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(PACKAGE_PART, file, descriptor)

/**
 * @param representativeFile one of the files representing this multifile class (will be used for diagnostics)
 */
fun MultifileClass(representativeFile: KtFile?, descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin =
        JvmDeclarationOrigin(MULTIFILE_CLASS, representativeFile, descriptor)
fun MultifileClassPart(file: KtFile, descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin =
        JvmDeclarationOrigin(MULTIFILE_CLASS_PART, file, descriptor)

fun DefaultImpls(element: PsiElement?, descriptor: ClassDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(INTERFACE_DEFAULT_IMPL, element, descriptor)

fun Delegation(element: PsiElement?, descriptor: FunctionDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(DELEGATION, element, descriptor)

fun SamDelegation(descriptor: FunctionDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(SAM_DELEGATION, null, descriptor)

fun Synthetic(element: PsiElement?, descriptor: CallableMemberDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(SYNTHETIC, element, descriptor)

val CollectionStub = JvmDeclarationOrigin(COLLECTION_STUB, null, null)

fun AugmentedBuiltInApi(descriptor: CallableDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(AUGMENTED_BUILTIN_API, null, descriptor)
