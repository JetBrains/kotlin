/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.elements.KotlinValueClassRepresentation
import org.jetbrains.kotlin.psi.stubs.elements.KtClassElementType

class KotlinClassStubImpl(
    type: KtClassElementType,
    parent: StubElement<out PsiElement>?,
    private val qualifiedName: StringRef?,
    private val classId: ClassId?,
    private val name: StringRef?,
    private val superNames: Array<StringRef>,
    private val isInterface: Boolean,
    private val isEnumEntry: Boolean,
    private val isClsStubCompiledToJvmDefaultImplementation: Boolean,
    private val isLocal: Boolean,
    private val isTopLevel: Boolean,
    val valueClassRepresentation: KotlinValueClassRepresentation?,
) : KotlinStubBaseImpl<KtClass>(parent, type), KotlinClassStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedName) ?: return null
        return FqName(stringRef)
    }

    override fun isInterface(): Boolean = isInterface
    override fun isEnumEntry(): Boolean = isEnumEntry
    override fun isClsStubCompiledToJvmDefaultImplementation(): Boolean = isClsStubCompiledToJvmDefaultImplementation
    override fun isLocal(): Boolean = isLocal
    override fun getName(): String? = StringRef.toString(name)

    override fun getSuperNames(): List<String> = superNames.map(StringRef::toString)

    override fun getClassId(): ClassId? = classId

    override fun isTopLevel() = isTopLevel
}
