/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.declarations.asResolveState
import org.jetbrains.kotlin.fir.declarations.impl.FirScriptImpl
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirScriptBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var name: Name
    val statements: MutableList<FirStatement> = mutableListOf()
    lateinit var symbol: FirScriptSymbol
    val parameters: MutableList<FirVariable> = mutableListOf()
    val contextReceivers: MutableList<FirContextReceiver> = mutableListOf()

    override fun build(): FirScript {
        return FirScriptImpl(
            source,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            moduleData,
            origin,
            attributes,
            name,
            statements,
            symbol,
            parameters,
            contextReceivers.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildScript(init: FirScriptBuilder.() -> Unit): FirScript {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirScriptBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildScriptCopy(original: FirScript, init: FirScriptBuilder.() -> Unit): FirScript {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirScriptBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.name = original.name
    copyBuilder.statements.addAll(original.statements)
    copyBuilder.symbol = original.symbol
    copyBuilder.parameters.addAll(original.parameters)
    copyBuilder.contextReceivers.addAll(original.contextReceivers)
    return copyBuilder.apply(init).build()
}
