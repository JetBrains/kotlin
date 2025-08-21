/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirScriptImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirScriptBuilder : FirAnnotationContainerBuilder {
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var name: Name
    val declarations: MutableList<FirDeclaration> = mutableListOf()
    lateinit var source: KtSourceElement
    lateinit var symbol: FirScriptSymbol
    val parameters: MutableList<FirProperty> = mutableListOf()
    val receivers: MutableList<FirScriptReceiverParameter> = mutableListOf()
    var resultPropertyName: Name? = null

    override fun build(): FirScript {
        return FirScriptImpl(
            resolvePhase,
            annotations.toMutableOrEmpty(),
            moduleData,
            origin,
            attributes,
            name,
            declarations,
            source,
            symbol,
            parameters,
            receivers.toMutableOrEmpty(),
            resultPropertyName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildScript(init: FirScriptBuilder.() -> Unit): FirScript {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirScriptBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class, DirectDeclarationsAccess::class)
inline fun buildScriptCopy(original: FirScript, init: FirScriptBuilder.() -> Unit): FirScript {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirScriptBuilder()
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.name = original.name
    copyBuilder.declarations.addAll(original.declarations)
    copyBuilder.source = original.source
    copyBuilder.parameters.addAll(original.parameters)
    copyBuilder.receivers.addAll(original.receivers)
    copyBuilder.resultPropertyName = original.resultPropertyName
    return copyBuilder.apply(init).build()
}
