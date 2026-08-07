/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinObjectStubImpl(
    parent: StubElement<*>?,
    private val name: StringRef?,
    override val fqName: FqName?,
    override val classId: ClassId?,
    private val superNameRefs: Array<StringRef>,
    override val isTopLevel: Boolean,
    override val isLocal: Boolean,
    override val isObjectLiteral: Boolean,
    override val kdocText: String?,
    /**
     * Indicates if this object is a value object, or `null` if it is not one.
     * Only stubs built from compiled metadata have this information; it is always `null` for stubs built from sources.
     *
     * A value object never has underlying properties, so the representation is always an empty
     * [KotlinFullValueClassRepresentation].
     */
    val valueClassRepresentation: KotlinValueClassRepresentation?,
) : KotlinStubBaseImpl<KtObjectDeclaration>(parent, KtStubElementTypes.OBJECT_DECLARATION), KotlinObjectStub {
    override fun getName(): String? = name?.string
    override val superNames: List<String>
        get() = superNameRefs.map(StringRef::getString)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinObjectStubImpl = KotlinObjectStubImpl(
        parent = newParent,
        name = name,
        fqName = fqName,
        classId = classId,
        superNameRefs = superNameRefs,
        isTopLevel = isTopLevel,
        isLocal = isLocal,
        isObjectLiteral = isObjectLiteral,
        kdocText = kdocText,
        valueClassRepresentation = valueClassRepresentation,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinObjectStubImpl &&
                other.name == name &&
                other.fqName == fqName &&
                other.classId == classId &&
                other.isTopLevel == isTopLevel &&
                other.isLocal == isLocal &&
                other.isObjectLiteral == isObjectLiteral &&
                other.kdocText == kdocText &&
                other.valueClassRepresentation == valueClassRepresentation &&
                other.superNameRefs.contentEquals(superNameRefs)
}
