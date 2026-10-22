/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement

@KtImplementationDetail
class KotlinObjectStubImpl(
    parent: StubElement<*>?,
    private val name: StringRef?,
    override val fqName: FqName?,
    override val classId: ClassId?,
    private val superNameRefs: Array<StringRef>,
    override val isTopLevel: Boolean,
    override val isObjectLiteral: Boolean,
    override val kdocText: String?,
    /**
     * The major version of the JVM class file the declaration is compiled to.
     * Only stubs built from class files have this information; it is always `null` for other stubs.
     */
    val classFileMajorVersion: Int?,
) : KotlinStubBaseImpl<KtObjectDeclaration>(parent, KtNodeTypes.OBJECT_DECLARATION), KotlinObjectStub {
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
        isObjectLiteral = isObjectLiteral,
        kdocText = kdocText,
        classFileMajorVersion = classFileMajorVersion,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinObjectStubImpl &&
                other.name == name &&
                other.fqName == fqName &&
                other.classId == classId &&
                other.isTopLevel == isTopLevel &&
                other.isObjectLiteral == isObjectLiteral &&
                other.kdocText == kdocText &&
                other.classFileMajorVersion == classFileMajorVersion &&
                other.superNameRefs.contentEquals(superNameRefs)
}
