/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.light

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IFileElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

/**
 * Computation of qualified names for declarations in a [LighterAST], mirroring the PSI implementations in
 * `KtNamedDeclarationUtil` and `safeFqNameForLazyResolve` from `ktPsiUtil.kt`.
 *
 * The walks follow the *tree* parent chain (not the stub parent chain) because the stub chain skips unstubbed
 * parents such as blocks, which would make local declarations appear non-local.
 */

/** The package name of the file, mirroring `KtPackageDirective.getFqName` on the file's package directive. */
internal fun LighterAST.packageFqName(): FqName {
    val directive = firstChildOfType(root, KtNodeTypes.PACKAGE_DIRECTIVE) ?: return FqName.ROOT
    val nameExpression = firstChildOfType(directive, PACKAGE_NAME_EXPRESSION_TYPES) ?: return FqName.ROOT
    val qualifiedName = qualifiedNameFromDottedExpression(nameExpression) ?: return FqName.ROOT
    return if (qualifiedName.isEmpty()) FqName.ROOT else FqName(qualifiedName)
}

private val PACKAGE_NAME_EXPRESSION_TYPES = com.intellij.psi.tree.TokenSet.create(
    KtNodeTypes.REFERENCE_EXPRESSION,
    KtNodeTypes.DOT_QUALIFIED_EXPRESSION,
)

/**
 * Builds the dot-separated name from a (possibly nested) dot-qualified chain of simple name references, mirroring
 * `KtPackageDirective.getQualifiedNameOf`. Returns `null` for an unexpected expression shape.
 */
private fun LighterAST.qualifiedNameFromDottedExpression(expression: LighterASTNode): String? {
    val names = ArrayDeque<String>()
    var current: LighterASTNode? = expression
    while (current != null && current.tokenType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION) {
        val selector = qualifiedSelector(current)
        if (selector != null && selector.tokenType == KtNodeTypes.REFERENCE_EXPRESSION) {
            names.addFirst(referencedName(selector, NAME_REFERENCE_NAME_ELEMENT_TYPES))
        }
        current = firstExpressionChild(current)
    }

    if (current != null && current.tokenType == KtNodeTypes.REFERENCE_EXPRESSION) {
        names.addFirst(referencedName(current, NAME_REFERENCE_NAME_ELEMENT_TYPES))
    }

    return names.joinToString(".")
}

/**
 * The qualified name of a named declaration using safe names, mirroring `safeFqNameForLazyResolve`.
 *
 * @param parentStub the stub the declaration's stub will be attached to; used to find the containing script's
 *  qualified name, which depends on the file name.
 */
internal fun LighterAST.safeFqNameForLazyResolve(declaration: LighterASTNode, parentStub: StubElement<*>): FqName? {
    val parent = getParent(declaration)
    val containingClassOrObject = parent
        ?.takeIf { it.tokenType == KtNodeTypes.CLASS_BODY }
        ?.let { containingClassOrObjectOfBody(it) }

    val parentFqName = if (
        containingClassOrObject != null &&
        containingClassOrObject.tokenType != KtNodeTypes.ENUM_ENTRY &&
        declaration.tokenType != KtNodeTypes.ENUM_ENTRY
    ) {
        safeFqNameForLazyResolve(containingClassOrObject, parentStub)
    } else {
        parentFqName(declaration, parentStub)
    }

    return parentFqName?.child(safeNameForLazyResolve(declaration))
}

/** Mirrors `KtNamedDeclaration.safeNameForLazyResolve`. */
private fun LighterAST.safeNameForLazyResolve(declaration: LighterASTNode): Name =
    anyDeclarationName(declaration)?.let(Name::identifier) ?: SpecialNames.NO_NAME_PROVIDED

/** The qualified name of a named declaration using real names only, mirroring `KtNamedDeclarationUtil.getFQName`. */
internal fun LighterAST.realFqName(declaration: LighterASTNode, parentStub: StubElement<*>): FqName? {
    val name = anyDeclarationName(declaration) ?: return null
    val parentFqName = parentFqName(declaration, parentStub) ?: return null
    return parentFqName.child(Name.identifier(name))
}

/** Element types whose PSI is a `KtClassOrObject`. */
private val CLASS_OR_OBJECT_TYPES = com.intellij.psi.tree.TokenSet.create(
    KtNodeTypes.CLASS,
    KtNodeTypes.OBJECT_DECLARATION,
    KtNodeTypes.ENUM_ENTRY,
)

/**
 * The class or object containing [declaration], mirroring `KtDeclaration.containingClassOrObject`.
 */
internal fun LighterAST.containingClassOrObject(declaration: LighterASTNode): LighterASTNode? {
    val parent = getParent(declaration) ?: return null
    return when {
        parent.tokenType == KtNodeTypes.CLASS_BODY -> containingClassOrObjectOfBody(parent)
        parent.tokenType in CLASS_OR_OBJECT_TYPES -> parent
        else -> null
    }
}

/**
 * The class or object owning a class body, mirroring `KtClassBody.containingClassOrObject`, which bypasses
 * `companion { }` blocks.
 */
private fun LighterAST.containingClassOrObjectOfBody(classBody: LighterASTNode): LighterASTNode? {
    var parent = getParent(classBody) ?: return null
    while (parent.tokenType == KtNodeTypes.COMPANION_BLOCK) {
        val outerBody = getParent(parent)?.takeIf { it.tokenType == KtNodeTypes.CLASS_BODY } ?: return null
        parent = getParent(outerBody) ?: return null
    }

    return parent.takeIf { it.tokenType in CLASS_OR_OBJECT_TYPES }
}

/**
 * The qualified name of the lexically enclosing scope of [declaration], mirroring
 * `KtNamedDeclarationUtil.getParentFqName`.
 */
internal fun LighterAST.parentFqName(declaration: LighterASTNode, parentStub: StubElement<*>): FqName? {
    var parent = getParent(declaration) ?: return null
    if (parent.tokenType == KtNodeTypes.CLASS_BODY) {
        // An enum entry body is an anonymous object, so its members are local
        if (getParent(parent)?.tokenType == KtNodeTypes.ENUM_ENTRY) return null

        // One nesting to KtClassBody doesn't affect the qualified name
        parent = containingClassOrObjectOfBody(parent) ?: return null
    }

    val parentType = parent.tokenType
    return when {
        parentType is IFileElementType -> packageFqName()

        parentType == KtNodeTypes.FUN ||
                parentType == KtNodeTypes.CLASS ||
                parentType == KtNodeTypes.ENUM_ENTRY ||
                parentType == KtNodeTypes.OBJECT_DECLARATION ->
            realFqName(parent, parentStub)

        declaration.tokenType == KtNodeTypes.VALUE_PARAMETER -> {
            val constructorClass = classIfParameterIsProperty(declaration) ?: return null
            realFqName(constructorClass, parentStub)
        }

        parentType == KtNodeTypes.BLOCK && getParent(parent)?.tokenType == KtNodeTypes.SCRIPT ->
            scriptFqName(parentStub)

        else -> null
    }
}

/**
 * The class whose primary constructor declares [parameter] as a property (`val`/`var`), mirroring
 * `KtPsiUtil.getClassIfParameterIsProperty`.
 */
private fun LighterAST.classIfParameterIsProperty(parameter: LighterASTNode): LighterASTNode? {
    val hasValOrVar = firstChildOfType(parameter, VAL_VAR_TOKENS) != null
    if (!hasValOrVar) return null

    val parameterList = getParent(parameter) ?: return null
    val constructor = getParent(parameterList) ?: return null
    if (constructor.tokenType != KtNodeTypes.PRIMARY_CONSTRUCTOR) return null

    return getParent(constructor)?.takeIf { it.tokenType in CLASS_OR_OBJECT_TYPES }
}

internal val VAL_VAR_TOKENS = com.intellij.psi.tree.TokenSet.create(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)

/**
 * The qualified name of the containing script, mirroring `KtScript.getFqName`: the file's package plus a name
 * derived from the file name. The enclosing script stub is created before its members, so the name is taken from
 * the stub parent chain.
 */
internal fun scriptFqName(parentStub: StubElement<*>): FqName? {
    var stub: StubElement<*>? = parentStub
    while (stub != null) {
        if (stub is KotlinScriptStub) return stub.fqName
        stub = stub.parentStub
    }
    return null
}

/**
 * The qualified name for a fresh script stub, mirroring `KtScript.getFqName` computed from the file: the file's
 * package plus a name derived from the file name. The file is found through the stub parent chain, where the file
 * stub is created before the script stub.
 */
internal fun scriptFqNameForStub(parentStub: StubElement<*>): FqName {
    var stub: StubElement<*>? = parentStub
    while (stub != null && stub !is KotlinFileStub) {
        stub = stub.parentStub
    }

    val fileStub = stub ?: return FqName.ROOT.child(NameUtils.getScriptNameForFile("script"))
    val fileName = (fileStub as? KotlinFileStubImpl)?.psi?.name ?: "script"
    return fileStub.getPackageFqName().child(NameUtils.getScriptNameForFile(fileName))
}
