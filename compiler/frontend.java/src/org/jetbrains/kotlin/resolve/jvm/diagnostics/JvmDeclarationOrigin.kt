/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.resolve.jvm.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPureElement
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.*

enum class MemberKind { FIELD, METHOD }

data class RawSignature(val name: String, val desc: String, val kind: MemberKind)

open class JvmDeclarationOrigin(
    val originKind: JvmDeclarationOriginKind,
    val element: PsiElement?,
    val descriptor: DeclarationDescriptor?,
    val parametersForJvmOverload: List<KtParameter?>? = null
) {
    // This property is used to get the original element in the sources, from which this declaration was generated.
    // In the old JVM backend, it is just the PSI element. In JVM IR, it is the original IR element (before any deep copy).
    open val originalSourceElement: Any?
        get() = element

    override fun toString(): String =
        if (this == NO_ORIGIN) "NO_ORIGIN" else "origin=$originKind element=${element?.javaClass?.simpleName} descriptor=$descriptor"

    companion object {
        @JvmField
        val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin(OTHER, null, null)
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

fun OtherOrigin(descriptor: DeclarationDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(OTHER, DescriptorToSourceUtils.descriptorToDeclaration(descriptor), descriptor)

fun Bridge(
    descriptor: DeclarationDescriptor,
    element: PsiElement? = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
): JvmDeclarationOrigin =
    JvmDeclarationOrigin(BRIDGE, element, descriptor)

fun PackagePart(file: KtFile, descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(PACKAGE_PART, file, descriptor)

/**
 * @param representativeFile one of the files representing this multifile class (will be used for diagnostics)
 */
fun MultifileClass(representativeFile: KtFile?, descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(MULTIFILE_CLASS, representativeFile, descriptor)

fun MultifileClassPart(file: KtFile, descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(MULTIFILE_CLASS_PART, file, descriptor)

fun DefaultImpls(element: PsiElement?, descriptor: ClassDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(INTERFACE_DEFAULT_IMPL, element, descriptor)

fun Delegation(element: PsiElement?, descriptor: FunctionDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(DELEGATION, element, descriptor)

fun SamDelegation(descriptor: FunctionDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(SAM_DELEGATION, null, descriptor)

fun Synthetic(element: PsiElement?, descriptor: DeclarationDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(SYNTHETIC, element, descriptor)

val CollectionStub = JvmDeclarationOrigin(COLLECTION_STUB, null, null)

fun AugmentedBuiltInApi(descriptor: CallableDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(AUGMENTED_BUILTIN_API, null, descriptor)

fun UnboxMethodOfInlineClass(descriptor: FunctionDescriptor): JvmDeclarationOrigin =
    JvmDeclarationOrigin(UNBOX_METHOD_OF_INLINE_CLASS, null, descriptor)
