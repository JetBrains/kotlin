/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.builder.FirDeclarationBuilder
import org.jetbrains.kotlin.metadata.ProtoBuf

private object KDocTextKey : FirDeclarationDataKey()

val FirDeclaration.kdocText: String?
        by FirDeclarationDataRegistry.data(KDocTextKey)

interface FirKDocDeserializer : FirSessionComponent {
    fun loadPropertyKDoc(proto: ProtoBuf.Property): String?
    fun loadFunctionKDoc(proto: ProtoBuf.Function): String?
    fun loadConstructorKDoc(proto: ProtoBuf.Constructor): String?
    fun loadClassKDoc(proto: ProtoBuf.Class): String?

    object Empty : FirKDocDeserializer {
        override fun loadPropertyKDoc(proto: ProtoBuf.Property): String? = null
        override fun loadFunctionKDoc(proto: ProtoBuf.Function): String? = null
        override fun loadConstructorKDoc(proto: ProtoBuf.Constructor): String? = null
        override fun loadClassKDoc(proto: ProtoBuf.Class): String? = null
    }
}

val FirSession.kdocDeserializer: FirKDocDeserializer? by FirSession.nullableSessionComponentAccessor()

val FirSession.effectiveKdocDeserializer: FirKDocDeserializer
    get() = kdocDeserializer ?: FirKDocDeserializer.Empty

private var FirDeclarationAttributes.kdocText: String?
        by FirDeclarationDataRegistry.attributesAccessor(KDocTextKey)

internal fun FirDeclarationBuilder.applyKDoc(text: String?) {
    if (text != null) {
        attributes.kdocText = text
    }
}