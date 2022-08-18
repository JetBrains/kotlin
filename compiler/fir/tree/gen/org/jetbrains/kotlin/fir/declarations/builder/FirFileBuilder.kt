/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirFileImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirFileBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var moduleData: FirModuleData
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var packageDirective: FirPackageDirective
    val imports: MutableList<FirImport> = mutableListOf()
    val declarations: MutableList<FirDeclaration> = mutableListOf()
    lateinit var name: String
    var sourceFile: KtSourceFile? = null
    var sourceFileLinesMapping: KtSourceFileLinesMapping? = null

    override fun build(): FirFile {
        return FirFileImpl(
            source,
            annotations,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            packageDirective,
            imports,
            declarations,
            name,
            sourceFile,
            sourceFileLinesMapping,
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
