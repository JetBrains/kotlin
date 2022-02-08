/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import java.io.File

internal object ResolveTreeBuilder {

    private val file: File? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        System.getProperty("fir.lazy.resolve.dump")?.let {
            File(it)
        }
    }

    class Context(var rootCall: Boolean = true, var underLock: Boolean = false, val builder: StringBuilder = StringBuilder())

    val currentContext: ThreadLocal<Context> = ThreadLocal.withInitial { Context() }

    private fun FirResolvePhase.firPhaseName(): String = when (this) {
        FirResolvePhase.RAW_FIR -> "Raw"
        FirResolvePhase.IMPORTS -> "Imports"
        FirResolvePhase.SUPER_TYPES -> "SuperTypes"
        FirResolvePhase.TYPES -> "Types"
        FirResolvePhase.STATUS -> "Status"
        FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS -> "ArgumentsOfAnnotations"
        FirResolvePhase.CONTRACTS -> "Contracts"
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> "ImplicitTypes"
        FirResolvePhase.BODY_RESOLVE -> "Body"
        else -> "?"
    }

    fun resolvePhase(
        declaration: FirDeclaration,
        phase: FirResolvePhase,
        body: () -> Unit
    ) {
        newNodeContext {
            resolveToDeclaration(declaration, "Phase", false, phase) {
                body()
                declaration
            }
        }
    }

    fun resolveEnsure(
        declaration: FirDeclaration,
        phase: FirResolvePhase,
        body: () -> Unit
    ) {
        newNodeContext {
            resolveToDeclaration(declaration, "Ensure", true, phase) {
                body()
                declaration
            }
        }
    }

    inline fun <T> lockNode(startTime: Long, body: () -> T): T {
        if (file == null) return body()
        val waitEnd = System.currentTimeMillis()
        val currentContext = currentContext.get()
        val builderLength = currentContext.builder.length
        val wasUnderLock = currentContext.underLock
        currentContext.underLock = true
        return newNodeContext {
            try {
                body()
            } finally {
                val contentionTime = waitEnd - startTime
                val executionTime = System.currentTimeMillis() - waitEnd
                if (!wasUnderLock && builderLength != currentContext.builder.length) {
                    val tag = "<UnderLock waitTime=\"$contentionTime\" executionTime=\"$executionTime\">"
                    currentContext.builder.insert(builderLength, tag)
                    currentContext.builder.append("</UnderLock>")
                }
                currentContext.underLock = wasUnderLock
            }
        }
    }

    fun <T : FirDeclaration> resolveReturnTypeCalculation(
        declaration: FirDeclaration,
        body: () -> T
    ): T = newNodeContext {
        resolveToDeclaration(declaration, "RTC", true, null, body)
    }

    private fun FirDeclaration.toDeclarationName(): String = when (val symbol = symbol) {
        is FirClassSymbol<*> -> symbol.classId.asSingleFqName().asString()
        is FirCallableSymbol<*> -> symbol.callableId.asSingleFqName().asString()
        is FirFileSymbol -> symbol.fir.name
        else -> symbol::class.qualifiedName
    } ?: "${this::class.qualifiedName}:${hashCode()}"

    private fun <T : FirDeclaration> resolveToDeclaration(
        declaration: FirDeclaration,
        nodeName: String,
        needWriteDeclarationName: Boolean,
        phase: FirResolvePhase? = null,
        body: () -> T
    ): T {
        if (phase != null && declaration.resolvePhase >= phase) return body()

        val currentContext = currentContext.get()
        val currentPosition = currentContext.builder.length
        var withException = true
        val start = System.currentTimeMillis()
        try {
            return body().also {
                withException = false
            }
        } finally {
            val end = System.currentTimeMillis()
            val timeAttr = " time=\"${end - start}\""
            val phaseAttr = if (phase != null) " phase=\"${phase.firPhaseName()}\"" else ""
            val declarationAttr = if (needWriteDeclarationName) " declaration=\"${declaration.toDeclarationName()}\"" else ""
            val exceptionAttr = if (withException) " withException=\"true\"" else ""
            val nodeContent = "$declarationAttr$phaseAttr$timeAttr$exceptionAttr"

            if (currentPosition == currentContext.builder.length) {
                currentContext.builder.append("<$nodeName$nodeContent />")
            } else {
                currentContext.builder.insert(currentPosition, "<$nodeName$nodeContent>")
                currentContext.builder.append("</$nodeName>")
            }
        }
    }

    private inline fun <T> newNodeContext(body: () -> T): T {
        val dumpFile = file ?: return body()
        val currentContext = currentContext.get()
        val oldRootEnsure = currentContext.rootCall
        try {
            currentContext.rootCall = false
            return body()
        } finally {
            currentContext.rootCall = oldRootEnsure
            if (oldRootEnsure) {
                synchronized(dumpFile) {
                    dumpFile.appendText(currentContext.builder.toString())
                }
                currentContext.builder.clear()
            }
        }
    }
}