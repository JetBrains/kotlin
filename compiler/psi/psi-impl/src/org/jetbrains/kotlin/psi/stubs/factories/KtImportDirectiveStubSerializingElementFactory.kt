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
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportDirectiveStubImpl
import org.jetbrains.kotlin.psi.stubs.light.NAME_REFERENCE_NAME_ELEMENT_TYPES
import org.jetbrains.kotlin.psi.stubs.light.firstChildOfType
import org.jetbrains.kotlin.psi.stubs.light.hasErrorElements
import org.jetbrains.kotlin.psi.stubs.light.isExpression
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
        val segments = ArrayList<String>()
        if (!collectSegments(tree, reference, segments)) return null
        return FqName.fromSegments(segments)
    }

    /**
     * Appends the segments of the qualified name [expression] stands for to [segments] and returns `true`, or returns `false` without
     * appending anything if `fqNameFromExpression` would return `null`: a qualified expression whose selector is not a simple name
     * contributes its receiver only. The name is built at once, as chaining [FqName.child] copies the prefix for each segment.
     */
    private fun collectSegments(tree: LighterAST, expression: LighterASTNode, segments: MutableList<String>): Boolean =
        when (expression.tokenType) {
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                // The receiver is the first expression child, and the selector is the first expression after the dot (see
                // `qualifiedSelector`); both are found in a single pass, as a light tree rebuilds the children on every access
                var receiver: LighterASTNode? = null
                var selector: LighterASTNode? = null
                var seenOperation = false
                for (child in tree.getChildren(expression)) {
                    val type = child.tokenType
                    when {
                        !seenOperation && (type == KtTokens.DOT || type == KtTokens.SAFE_ACCESS) -> seenOperation = true
                        isExpression(child) -> {
                            if (receiver == null) receiver = child
                            if (seenOperation) {
                                selector = child
                                break
                            }
                        }
                    }
                }

                val hasReceiverName = receiver != null && collectSegments(tree, receiver, segments)
                val name = selector
                    ?.takeIf { it.tokenType == KtNodeTypes.REFERENCE_EXPRESSION }
                    ?.let { tree.referencedName(it, NAME_REFERENCE_NAME_ELEMENT_TYPES) }

                when {
                    name == null -> hasReceiverName
                    hasReceiverName -> {
                        segments += name
                        true
                    }
                    else -> false
                }
            }
            KtNodeTypes.REFERENCE_EXPRESSION -> {
                segments += tree.referencedName(expression, NAME_REFERENCE_NAME_ELEMENT_TYPES)
                true
            }
            else -> false
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
