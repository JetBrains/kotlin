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
    override val kdocText: String?
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
    )
}
