/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets

object StubUtils {
    @JvmStatic
    fun deserializeClassId(dataStream: StubInputStream): ClassId? {
        val classId = dataStream.readName() ?: return null
        return ClassId.fromString(classId.string)
    }

    @JvmStatic
    fun serializeClassId(dataStream: StubOutputStream, classId: ClassId?) {
        dataStream.writeName(classId?.asString())
    }

    @JvmStatic
    fun createNestedClassId(parentStub: StubElement<*>, currentDeclaration: KtClassLikeDeclaration): ClassId? {
        if (currentDeclaration is KtObjectDeclaration && currentDeclaration.isObjectLiteral()) {
            return null
        }

        return when (parentStub) {
            is KotlinFileStub -> ClassId(parentStub.getPackageFqName(), currentDeclaration.nameAsSafeName)
            is KotlinScriptStub -> createNestedClassId(parentStub.parentStub, currentDeclaration)
            is KotlinPlaceHolderStub<*> if parentStub.stubType == KtStubElementTypes.CLASS_BODY -> {
                val containingClassStub = parentStub.parentStub as? KotlinClassifierStub
                if (containingClassStub != null && currentDeclaration !is KtEnumEntry) {
                    containingClassStub.classId?.createNestedClassId(currentDeclaration.nameAsSafeName)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    @JvmStatic
    internal tailrec fun isDeclaredInsideValueArgument(node: ASTNode?): Boolean {
        val parent = node?.treeParent
        return when (parent?.elementType) {
            // Constants are allowed only in the argument position
            KtStubElementTypes.VALUE_ARGUMENT -> true
            null, in KtTokenSets.DECLARATION_TYPES -> false
            else -> isDeclaredInsideValueArgument(parent)
        }
    }

    @JvmStatic
    internal fun StubOutputStream.writeNullableBoolean(value: Boolean?) {
        val byte = when (value) {
            true -> 0
            false -> 1
            null -> 2
        }

        writeByte(byte)
    }

    @JvmStatic
    internal fun StubInputStream.readNullableBoolean(): Boolean? = when (readByte().toInt()) {
        0 -> true
        1 -> false
        else -> null
    }

    @JvmStatic
    internal fun StubInputStream.readFqName(): FqName = FqName(readNameString()!!)
}
