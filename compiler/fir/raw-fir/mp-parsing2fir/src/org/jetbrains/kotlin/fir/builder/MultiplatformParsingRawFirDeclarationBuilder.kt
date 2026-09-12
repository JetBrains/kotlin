/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.builder.FirScriptBuilder
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lightTree.converter.TreeRawFirDeclarationBuilderProxy
import org.jetbrains.kotlin.fir.lightTree.converter.TreeRawFirExpressionBuilderProxy
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.kmp.tree.LightNode

class MultiplatformParsingRawFirDeclarationBuilder(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    tree: KotlinLightTreeStructure,
    context: Context<LightNode> = Context(),
) : AbstractMultiplatformParsingRawFirBuilder(session, tree, context) {

    private val headerMode = session.languageVersionSettings.getFlag(AnalysisFlags.headerMode)
    private val expressionConverter = TreeRawFirExpressionBuilderProxy(this, context, baseModuleData, headerMode)
    private val declarationConverter = TreeRawFirDeclarationBuilderProxy(
        this, context, baseModuleData, expressionConverter, headerMode, baseScopeProvider
    )

    init {
        expressionConverter.declarationBuilder = declarationConverter
    }

    fun convertFile(file: LightNode, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping): FirFile {
        return declarationConverter.convertFile(file, sourceFile, linesMapping)
    }

    override fun convertScript(
        script: LightNode,
        scriptSource: KtSourceElement,
        fileName: String,
        setup: FirScriptBuilder.() -> Unit
    ): FirScript {
        return declarationConverter.convertScript(script, scriptSource, fileName, setup)
    }
}
