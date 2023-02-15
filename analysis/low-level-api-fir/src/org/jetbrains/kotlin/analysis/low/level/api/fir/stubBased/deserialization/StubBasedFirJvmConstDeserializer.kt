/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty

class StubBasedFirJvmConstDeserializer(
    session: FirSession,
    private val binaryClass: KotlinJvmBinaryClass
) : FirConstDeserializer(session) {
    override fun loadConstant(property: KtProperty, callableId: CallableId): FirExpression? {
        if (!property.hasModifier(KtTokens.CONST_KEYWORD)) return null
        constantCache[callableId]?.let { return it }

        binaryClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
            override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? = null

            override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                if (initializer != null) {
                    val constant = buildFirConstant(null, initializer, desc)
                    constant?.let { constantCache[callableId.replaceName(name)] = it }
                }
                return null
            }
        }, null)

        return constantCache[callableId]
    }
}