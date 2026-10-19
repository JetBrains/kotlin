/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportDirectiveStubImpl
import org.jetbrains.kotlin.psi.stubs.light.NAME_REFERENCE_NAME_ELEMENT_TYPES
import org.jetbrains.kotlin.psi.stubs.light.firstChildOfType
import org.jetbrains.kotlin.psi.stubs.light.firstExpressionChild
import org.jetbrains.kotlin.psi.stubs.light.hasErrorElements
import org.jetbrains.kotlin.psi.stubs.light.qualifiedSelector
import org.jetbrains.kotlin.psi.stubs.light.referencedName

internal object KtImportDirectiveStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinImportDirectiveStubImpl, KtImportDirective>(
        type = KtNodeTypes.IMPORT_DIRECTIVE,
    ) {

    override fun createPsi(stub: KotlinImportDirectiveStubImpl): KtImportDirective = KtImportDirective(stub)

    override fun createStub(
        psi: KtImportDirective,
        parentStub: StubElement<*>?,
    ): KotlinImportDirectiveStubImpl = KotlinImportDirectiveStubImpl(
        parent = parentStub,
        isAllUnder = psi.isAllUnder,
        importedFqNameRef = StringRef.fromString(psi.importedFqName?.asString()),
        isValid = psi.isValidImport,
    )

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinImportDirectiveStubImpl = KotlinImportDirectiveStubImpl(
        parent = parentStub,
        isAllUnder = tree.firstChildOfType(node, KtTokens.MUL) != null,
        importedFqNameRef = StringRef.fromString(importedFqName(tree, node)?.asString()),
        isValid = !tree.hasErrorElements(node),
    )

    /** Mirrors `KtImportDirective.getImportedFqName` and its `fqNameFromExpression`. */
    private fun importedFqName(tree: LighterAST, node: LighterASTNode): FqName? {
        val reference = tree.firstChildOfType(node, KtTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS) ?: return null
        return fqNameFromExpression(tree, reference)
    }

    private fun fqNameFromExpression(tree: LighterAST, expression: LighterASTNode): FqName? = when (expression.tokenType) {
        KtNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
            val parentFqName = tree.firstExpressionChild(expression)?.let { fqNameFromExpression(tree, it) }
            val selector = tree.qualifiedSelector(expression)
            val name = selector
                ?.takeIf { it.tokenType == KtNodeTypes.REFERENCE_EXPRESSION }
                ?.let { Name.identifier(tree.referencedName(it, NAME_REFERENCE_NAME_ELEMENT_TYPES)) }
            when {
                name == null -> parentFqName
                parentFqName != null -> parentFqName.child(name)
                else -> null
            }
        }
        KtNodeTypes.REFERENCE_EXPRESSION ->
            FqName.topLevel(Name.identifier(tree.referencedName(expression, NAME_REFERENCE_NAME_ELEMENT_TYPES)))
        else -> null
    }

    override fun serialize(stub: KotlinImportDirectiveStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isAllUnder)
        dataStream.writeName(stub.importedFqName?.asString())
        dataStream.writeBoolean(stub.isValid)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinImportDirectiveStubImpl = KotlinImportDirectiveStubImpl(
        parent = parentStub,
        isAllUnder = dataStream.readBoolean(),
        importedFqNameRef = dataStream.readName(),
        isValid = dataStream.readBoolean(),
    )
}
