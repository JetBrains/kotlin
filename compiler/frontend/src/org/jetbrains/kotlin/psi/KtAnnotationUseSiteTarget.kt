/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationUseSiteTargetStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtAnnotationUseSiteTarget : KtElementImplStub<KotlinAnnotationUseSiteTargetStub> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinAnnotationUseSiteTargetStub) : super(stub, KtStubElementTypes.ANNOTATION_TARGET)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitAnnotationUseSiteTarget(this, data)

    fun getAnnotationUseSiteTarget(): AnnotationUseSiteTarget {
        val targetString = stub?.getUseSiteTarget()
        if (targetString != null) {
            try {
                return AnnotationUseSiteTarget.valueOf(targetString)
            } catch (e: IllegalArgumentException) {
                // Ok, resolve via node tree
            }
        }

        val node = firstChild.node
        return when (node.elementType) {
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