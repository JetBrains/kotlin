/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    override val classId: ClassId?,
    private val name: StringRef?,
    private val superNameRefs: Array<StringRef>,
    override val isInterface: Boolean,
    override val isEnumEntry: Boolean,
    override val isClsStubCompiledToJvmDefaultImplementation: Boolean,
    override val isLocal: Boolean,
    override val isTopLevel: Boolean,
    val valueClassRepresentation: KotlinValueClassRepresentation?,
) : KotlinStubBaseImpl<KtClass>(parent, type), KotlinClassStub {

    override val fqName: FqName?
        get() {
            val stringRef = StringRef.toString(qualifiedName) ?: return null
            return FqName(stringRef)
        }

    override fun getName(): String? = StringRef.toString(name)

    override val superNames: List<String>
        get() = superNameRefs.map(StringRef::toString)
}
