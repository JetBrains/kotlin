/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
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
    AUGMENTED_BUILTIN_API,
    ERASED_INLINE_CLASS,
    UNBOX_METHOD_OF_INLINE_CLASS
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

@JvmOverloads
fun OtherOrigin(element: PsiElement?, descriptor: DeclarationDescriptor? = null) =
        if (element == null && descriptor == null)
            JvmDeclarationOrigin.NO_ORIGIN
        else
            JvmDeclarationOrigin(OTHER, element, descriptor)

@JvmOverloads
fun OtherOriginFromPure(element: KtPureElement?, descriptor: DeclarationDescriptor? = null) =
        OtherOrigin(element?.psiOrParent, descriptor)

fun OtherOrigin(descriptor: DeclarationDescriptor) = JvmDeclarationOrigin(OTHER, null, descriptor)

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

fun ErasedInlineClassOrigin(element: PsiElement?, descriptor: ClassDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(ERASED_INLINE_CLASS, element, descriptor)

fun UnboxMethodOfInlineClass(descriptor: FunctionDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(UNBOX_METHOD_OF_INLINE_CLASS, null, descriptor)
