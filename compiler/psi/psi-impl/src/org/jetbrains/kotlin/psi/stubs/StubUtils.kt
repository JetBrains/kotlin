/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.psi.PsiComment
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.stubs.StubUtils.searchForHasBackingFieldComment
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
    @Suppress("DEPRECATION")
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

    /**
     * `/* hasBackingField: true */` or `/* hasBackingField: false */` are special comments added during conversion
     * from a metadata to a decompiled text. This decompiled text is then used to create decompiled stubs.
     *
     * This is a reliable way to pass arbitrary information from the decompiler to the stub builder as the Analysis API
     * controls both the decompiler and the stub builder, so they don't interfere with user code/comments.
     *
     * @see KotlinPropertyStub.hasBackingField
     */
    @JvmStatic
    internal fun searchForHasBackingFieldComment(property: KtProperty): Boolean? {
        if (!property.containingKtFile.isCompiled) {
            return null
        }

        var child = property.firstChild
        while (child != null) {
            if (child is PsiComment) {
                searchForHasBackingField(child)?.let { return it }
            }

            child = child.nextSibling
        }

        return null
    }

    @OptIn(IntellijInternalApi::class)
    private fun searchForHasBackingField(comment: PsiComment): Boolean? {
        if (comment.tokenType != KtTokens.BLOCK_COMMENT) {
            return null
        }

        val textLength = comment.textLength
        if (textLength != HAS_BACKING_FIELD_COMMENT_TRUE_LENGTH && textLength != HAS_BACKING_FIELD_COMMENT_FALSE_LENGTH) {
            return null
        }

        val text = comment.text
        return if (text.startsWith(HAS_BACKING_FIELD_COMMENT_PREFIX)) {
            text[HAS_BACKING_FIELD_COMMENT_VALUE_START_INDEX] == 't'
        } else {
            null
        }
    }

    /** @see searchForHasBackingFieldComment */
    @IntellijInternalApi
    const val HAS_BACKING_FIELD_COMMENT_PREFIX: String = "/* hasBackingField: "

    /**
     * The index of `t` or `f` in the special comment
     */
    @OptIn(IntellijInternalApi::class)
    private const val HAS_BACKING_FIELD_COMMENT_VALUE_START_INDEX: Int = HAS_BACKING_FIELD_COMMENT_PREFIX.length

    @OptIn(IntellijInternalApi::class)
    private const val HAS_BACKING_FIELD_COMMENT_TRUE: String = HAS_BACKING_FIELD_COMMENT_PREFIX + "${true} */"
    private const val HAS_BACKING_FIELD_COMMENT_TRUE_LENGTH: Int = HAS_BACKING_FIELD_COMMENT_TRUE.length

    private const val HAS_BACKING_FIELD_COMMENT_FALSE_LENGTH: Int =
        HAS_BACKING_FIELD_COMMENT_TRUE_LENGTH - true.toString().length + false.toString().length
}
