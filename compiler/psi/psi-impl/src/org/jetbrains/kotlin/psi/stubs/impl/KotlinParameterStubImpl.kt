/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.stubs.KotlinParameterStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

/**
 * @param equalityBoundType The equality bound of an `operator fun equals` parameter, see `kotlin.EqualityBound`.
 * It is only present in stubs built from binaries, as the bound might be inherited from an overridden `equals`, and so is not necessarily
 * spelled out by an annotation on this parameter itself.
 * @param kdocText Raw KDoc text of the property this parameter declares, if available.
 * As with every other declaration, it is only present in stubs built from binaries.
 * @param constantInitializer The compile-time constant the property this parameter declares is initialized with.
 * It is only present in stubs built from binaries, where the value comes from the metadata rather than from the sources.
 */
@OptIn(KtImplementationDetail::class)
class KotlinParameterStubImpl(
    parent: StubElement<*>?,
    private val fqNameRef: StringRef?,
    private val name: StringRef?,
    override val isMutable: Boolean,
    override val hasValOrVar: Boolean,
    override val hasDefaultValue: Boolean,
    val functionTypeParameterName: String?,
    val equalityBoundType: KotlinTypeBean?,
    val kdocText: String?,
    val constantInitializer: ConstantValue<*>?,
) : KotlinStubBaseImpl<KtParameter>(parent, KtStubElementTypes.VALUE_PARAMETER), KotlinParameterStub {

    override fun getName(): String? = name?.string

    // val/var parameters from a primary constructor might have fqName
    override val fqName: FqName?
        get() = fqNameRef?.string?.let(::FqName)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinParameterStubImpl = KotlinParameterStubImpl(
        parent = newParent,
        fqNameRef = fqNameRef,
        name = name,
        isMutable = isMutable,
        hasValOrVar = hasValOrVar,
        hasDefaultValue = hasDefaultValue,
        functionTypeParameterName = functionTypeParameterName,
        equalityBoundType = equalityBoundType,
        kdocText = kdocText,
        constantInitializer = constantInitializer,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinParameterStubImpl &&
                other.name == name &&
                other.fqNameRef == fqNameRef &&
                other.isMutable == isMutable &&
                other.hasValOrVar == hasValOrVar &&
                other.hasDefaultValue == hasDefaultValue &&
                other.functionTypeParameterName == functionTypeParameterName &&
                other.equalityBoundType == equalityBoundType &&
                other.kdocText == kdocText &&
                other.constantInitializer == constantInitializer
}
