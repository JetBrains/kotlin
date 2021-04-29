/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirFileImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirFileBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    lateinit var declarationSiteSession: FirSession
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    val imports: MutableList<FirImport> = mutableListOf()
    val declarations: MutableList<FirDeclaration> = mutableListOf()
    lateinit var name: String
    lateinit var packageFqName: FqName

    override fun build(): FirFile {
        return FirFileImpl(
            source,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            annotations,
            imports,
            declarations,
            name,
            packageFqName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFile(init: FirFileBuilder.() -> Unit): FirFile {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirFileBuilder().apply(init).build()
}
