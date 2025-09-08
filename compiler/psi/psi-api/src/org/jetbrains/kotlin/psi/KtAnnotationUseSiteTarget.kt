/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationUseSiteTargetStub

class KtAnnotationUseSiteTarget : KtElementImplStub<KotlinAnnotationUseSiteTargetStub> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinAnnotationUseSiteTargetStub) : super(stub, KtStubBasedElementTypes.ANNOTATION_TARGET)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitAnnotationUseSiteTarget(this, data)

    fun getAnnotationUseSiteTarget(): AnnotationUseSiteTarget {
        val targetString = stub?.useSiteTarget
        if (targetString != null) {
            try {
                return AnnotationUseSiteTarget.valueOf(targetString)
            } catch (e: IllegalArgumentException) {
                // Ok, resolve via node tree
            }
        }

        val node = firstChild.node
        return when (node.elementType) {
            KtTokens.ALL_KEYWORD -> AnnotationUseSiteTarget.ALL
            KtTokens.FIELD_KEYWORD -> AnnotationUseSiteTarget.FIELD
            KtTokens.FILE_KEYWORD -> AnnotationUseSiteTarget.FILE
            KtTokens.PROPERTY_KEYWORD -> AnnotationUseSiteTarget.PROPERTY
            KtTokens.GET_KEYWORD -> AnnotationUseSiteTarget.PROPERTY_GETTER
            KtTokens.SET_KEYWORD -> AnnotationUseSiteTarget.PROPERTY_SETTER
            KtTokens.RECEIVER_KEYWORD -> AnnotationUseSiteTarget.RECEIVER
            KtTokens.PARAM_KEYWORD -> AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
            KtTokens.SETPARAM_KEYWORD -> AnnotationUseSiteTarget.SETTER_PARAMETER
            KtTokens.DELEGATE_KEYWORD -> AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
            else -> throw IllegalStateException("Unknown annotation target " + node.text)
        }
    }

}