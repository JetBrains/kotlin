/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.tree.IFileElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kmp.FullParserTestsWithLightTree
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.util.getChildren

// Normally LightTree eagerly parses blocks and lambdas but ignores KDoc
class LightTreeTestParser : AbstractTestParser<LighterASTNode>(ParseMode.NoKDoc), Disposable {
    private val disposable = Disposer.newDisposable("Disposable for the ${FullParserTestsWithLightTree::class.simpleName}")

    init {
        @OptIn(K1Deprecation::class)
        KotlinCoreEnvironment.createForTests(disposable, CompilerConfiguration.EMPTY, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private val builderFactoryInstance: PsiBuilderFactory = PsiBuilderFactory.getInstance()

    override fun parse(
        fileName: String,
        text: String,
    ): TestParseNode<LighterASTNode> {
        val builder = builderFactoryInstance.createBuilder(KotlinParserDefinition(), KotlinLexer(), text)
        val lightTree = KotlinLightParser.parse(builder, isScript(fileName))
        return lightTree.root.toTestParseTree(lightTree)
    }

    fun LighterASTNode.toTestParseTree(lightTree: FlyweightCapableTreeStructure<LighterASTNode>): TestParseNode<LighterASTNode> {
        // For some reason the root token differs in PSI and LightTree modes. Normalize it to PSI
        val normalizedToken = if (tokenType is IFileElementType) KtNodeTypes.KT_FILE else tokenType

        return TestParseNode(
            normalizedToken.toString(),
            startOffset,
            endOffset,
            this,
            getChildren(lightTree).map { it.toTestParseTree(lightTree) },
        )
    }

    override fun dispose() {
        ApplicationManager.getApplication().invokeLater {
            Disposer.dispose(disposable)
        }
    }
}
