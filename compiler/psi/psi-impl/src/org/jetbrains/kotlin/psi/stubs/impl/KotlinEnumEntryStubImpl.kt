/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinEnumEntryStubImpl(
    parent: StubElement<*>?,
    private val qualifiedName: StringRef?,
    private val name: StringRef?,
    override val isLocal: Boolean,
) : KotlinStubBaseImpl<KtClass>(
    parent = parent,
    elementType = KtStubElementTypes.ENUM_ENTRY,
), KotlinClassStub {
    override val isClsStubCompiledToJvmDefaultImplementation: Boolean
        get() = false

    override val isInterface: Boolean
        get() = false

    override val classId: ClassId?
        get() = null

    override val isTopLevel: Boolean
        get() = false

    override val fqName: FqName?
        get() = qualifiedName?.string?.let(::FqName)

    override fun getName(): String? = name?.string

    override val superNames: List<String>
        get() {
            if (findChildStubByType(KtStubElementTypes.INITIALIZER_LIST) == null) {
                return emptyList()
            }

            val enumClassStub = parentStub?.parentStub as? KotlinClassStub ?: error("Enum entry should have enum class parent")
            // Invalid code might have an enum class without a name
            val enumClassName = enumClassStub.name ?: SpecialNames.NO_NAME_PROVIDED.asString()
            return listOf(enumClassName)
        }

    override val kdocText: String?
        get() = null

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinEnumEntryStubImpl = KotlinEnumEntryStubImpl(
        parent = newParent,
        qualifiedName = qualifiedName,
        name = name,
        isLocal = isLocal,
    )
}
