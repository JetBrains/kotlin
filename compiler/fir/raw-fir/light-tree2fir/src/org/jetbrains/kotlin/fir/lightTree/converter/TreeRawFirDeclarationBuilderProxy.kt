/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import org.jetbrains.kotlin.BasicElementTypeId
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.NO_ID
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.firstFunctionCallInBlockHasLambdaArgumentWithLabel
import org.jetbrains.kotlin.fir.analysis.isCallTheFirstStatement
import org.jetbrains.kotlin.fir.analysis.isExpression
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirAnonymousFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirNamedFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirPrimaryConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirScriptBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousObject
import org.jetbrains.kotlin.fir.declarations.builder.buildBackingField
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.builder.buildDefaultSetterValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildEnumEntry
import org.jetbrains.kotlin.fir.declarations.builder.buildField
import org.jetbrains.kotlin.fir.declarations.builder.buildFile
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildScript
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeAlias
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.DanglingTypeConstraint
import org.jetbrains.kotlin.fir.declarations.utils.addDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.utils.danglingTypeConstraints
import org.jetbrains.kotlin.fir.declarations.utils.isCopiedDelegatedProperty
import org.jetbrains.kotlin.fir.declarations.utils.isScriptTopLevelDeclaration
import org.jetbrains.kotlin.fir.diagnostics.ConeContractMayNotHaveLabel
import org.jetbrains.kotlin.fir.diagnostics.ConeContractShouldBeFirstStatement
import org.jetbrains.kotlin.fir.diagnostics.ConeDanglingModifierOnTopLevel
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeNoConstructorError
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirWrappedDelegateExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.builder.buildEmptyExpressionBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.lightTree.fir.ClassWrapper
import org.jetbrains.kotlin.fir.lightTree.fir.DelegatedConstructorWrapper
import org.jetbrains.kotlin.fir.lightTree.fir.DestructuringDeclaration
import org.jetbrains.kotlin.fir.lightTree.fir.DestructuringEntry
import org.jetbrains.kotlin.fir.lightTree.fir.PrimaryConstructor
import org.jetbrains.kotlin.fir.lightTree.fir.TypeConstraint
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.lightTree.fir.addDestructuringStatements
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierList
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeParameterModifierList
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeProjectionModifierList
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDanglingModifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildDynamicTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildFunctionTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildIntersectionTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildPlaceholderProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.builder.buildUnionTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.KtNodeTypeId
import org.jetbrains.kotlin.KtTokenId
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class TreeRawFirDeclarationBuilderProxy<Node : Any, Type : Any>(
    val rootBuilder: AbstractTreeRawFirBuilder<Node, Type>,
    context: Context<Node>,
    session: FirSession,
    val expressionConverter: TreeRawFirExpressionBuilderProxy<Node, Type>,
    val headerMode: Boolean,
    val baseScopeProvider: FirScopeProvider,
) : AbstractTreeRawFirBuilder<Node, Type>(session, context) {
    private val keepPropertyBodiesInHeaderMode =
        session.languageVersionSettings.getFlag(AnalysisFlags.headerModeKeepPropertyBodies)

    /**
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parseFile]
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parsePreamble]
     */
    fun convertFile(file: Node, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping): FirFile {
        if (file.toTokenId() != KtNodeTypeId.FILE) {
            //TODO throw error
            throw Exception()
        }

        val fileSymbol = FirFileSymbol()
        val fileAnnotations = mutableListOf<FirAnnotation>()
        val importList = mutableListOf<FirImport>()
        val firDeclarationList = mutableListOf<FirDeclaration>()
        val modifierList = mutableListOf<Node>()
        val scriptNodes = mutableListOf<Node>()
        context.packageFqName = FqName.ROOT
        var packageDirective: FirPackageDirective? = null
        file.forEachChildren { child ->
            when (child.toTokenId()) {
                KtNodeTypeId.FILE_ANNOTATION_LIST -> {
                    context.withContainerSymbol(fileSymbol) {
                        convertAnnotationsOnlyTo(child, fileAnnotations)
                    }
                }
                KtNodeTypeId.PACKAGE_DIRECTIVE -> {
                    packageDirective = convertPackageDirective(child).also { context.packageFqName = it.packageFqName }
                }
                KtNodeTypeId.IMPORT_LIST -> importList += convertImportDirectives(child)
                KtNodeTypeId.CLASS, KtNodeTypeId.OBJECT_DECLARATION -> firDeclarationList += convertClass(child)
                KtNodeTypeId.FUNCTION -> firDeclarationList += convertFunctionDeclaration(child) as FirDeclaration
                KtNodeTypeId.PROPERTY -> firDeclarationList += convertPropertyDeclaration(child)
                KtNodeTypeId.TYPEALIAS -> firDeclarationList += convertTypeAlias(child)
                KtNodeTypeId.DESTRUCTURING_DECLARATION -> addNonLocalDestructuringDeclaration(firDeclarationList, child)
                KtNodeTypeId.SCRIPT -> scriptNodes += child
                KtNodeTypeId.MODIFIER_LIST -> modifierList += child
            }
        }

        modifierList.forEach {
            firDeclarationList += buildErrorNonLocalDeclarationForDanglingModifierList(it)
        }

        return buildFile {
            symbol = fileSymbol
            source = file.toFirSourceElement()
            origin = FirDeclarationOrigin.Source
            moduleData = baseModuleData
            name = sourceFile.name
            this.sourceFile = sourceFile
            this.sourceFileLinesMapping = linesMapping
            this.packageDirective = packageDirective ?: buildPackageDirective { packageFqName = context.packageFqName }
            annotations += fileAnnotations
            imports += importList
            for (scriptNode in scriptNodes) {
                firDeclarationList += convertScriptOrSnippets(scriptNode, sourceFile, this@buildFile)
            }
            declarations += firDeclarationList
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseBlockExpression
     */
    fun convertBlockExpression(
        node: Node,
        convertOnlyFirstStatement: Boolean = false,
    ): FirBlock {
        return convertBlockExpressionWithoutBuilding(node, convertOnlyFirstStatement = convertOnlyFirstStatement).build()
    }

    /**
     * @param convertOnlyFirstStatement Convert only the first statement of the block, which can be a contract, for header generation.
     */
    fun convertBlockExpressionWithoutBuilding(
        block: Node,
        kind: KtFakeSourceElementKind? = null,
        convertOnlyFirstStatement: Boolean = false,
    ): FirBlockBuilder {
        val firStatements = block.forEachChildrenReturnList { node, container ->
            if (!convertOnlyFirstStatement || container.isEmpty()) {
                when (node.toTokenId()) {
                    KtNodeTypeId.CLASS, KtNodeTypeId.OBJECT_DECLARATION -> container += convertClass(node) as FirStatement
                    KtNodeTypeId.FUNCTION -> container += convertFunctionDeclaration(node)
                    KtNodeTypeId.PROPERTY -> container += convertPropertyDeclaration(node) as FirStatement
                    KtNodeTypeId.DESTRUCTURING_DECLARATION -> container +=
                        convertDestructingDeclaration(node).toFirDestructingDeclaration(this, context, baseModuleData)
                    KtNodeTypeId.TYPEALIAS -> container += convertTypeAlias(node) as FirStatement
                    KtNodeTypeId.CLASS_INITIALIZER -> shouldNotBeCalled("CLASS_INITIALIZER expected to be processed during class body conversion")
                    else -> if (node.isExpression()) container += expressionConverter.getAsFirStatement(node)
                }
            }
        }
        return FirBlockBuilder().apply {
            source = block.toFirSourceElement(kind)
            firStatements.forEach { firStatement ->
                val isForLoopBlock = firStatement is FirBlock && firStatement.source?.kind == KtFakeSourceElementKind.DesugaredForLoop
                val isIncrementOrDecrement = firStatement is FirBlock
                        && firStatement.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement
                if (firStatement !is FirBlock || isForLoopBlock || firStatement.annotations.isNotEmpty() || isIncrementOrDecrement) {
                    statements += firStatement
                } else {
                    statements += firStatement.statements
                }
            }
        }
    }

    /*****    PREAMBLE    *****/
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePackageName
     */
    private fun convertPackageDirective(packageNode: Node): FirPackageDirective {
        var packageName: FqName = FqName.ROOT
        packageNode.forEachChildren {
            when (it.toTokenId()) {
                //TODO separate logic for both expression types
                KtNodeTypeId.DOT_QUALIFIED_EXPRESSION, KtNodeTypeId.REFERENCE_EXPRESSION -> packageName = parsePackageName(it)
            }
        }
        return buildPackageDirective {
            packageFqName = packageName
            source = packageNode.toFirSourceElement()
        }
    }

    private fun parsePackageName(node: Node): FqName {
        var packageName: FqName = FqName.ROOT
        val parts = parsePackageParts(node)

        for (part in parts) {
            packageName = packageName.child(Name.identifier(part))
        }

        return packageName
    }

    private fun parsePackageParts(node: Node): List<String> {
        fun parse(node: Node): MutableList<String> {
            if (node.toTokenId() == KtNodeTypeId.DOT_QUALIFIED_EXPRESSION) {
                val children = node.getChildren()

                if (children.size == 3) {
                    return parse(children.first()).apply {
                        add(children.last().getAsStringWithoutBacktick())
                    }
                }
            }

            if (node.toTokenId() == KtNodeTypeId.REFERENCE_EXPRESSION) {
                return mutableListOf(node.getAsStringWithoutBacktick())
            }

            return mutableListOf()
        }

        return parse(node)
    }

    private fun convertImportAlias(importAlias: Node): Pair<String, KtSourceElement>? {
        importAlias.forEachChildren {
            when (it.toTokenId()) {
                KtTokenId.IDENTIFIER -> return Pair(it.asText, it.toFirSourceElement())
            }
        }

        return null
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseImportDirective
     */
    private fun convertImportDirective(importDirective: Node): FirImport {
        var importedFqName: FqName? = null
        var isAllUnder = false
        var aliasName: String? = null
        var aliasSource: KtSourceElement? = null
        importDirective.forEachChildren { child ->
            when (child.toTokenId()) {
                KtNodeTypeId.REFERENCE_EXPRESSION, KtNodeTypeId.DOT_QUALIFIED_EXPRESSION -> {
                    importedFqName = mutableListOf<String>()
                        .apply { collectSegments(child) }
                        .joinToString(".")
                        .let { FqName(it) }
                }
                KtTokenId.MUL -> isAllUnder = true
                KtNodeTypeId.IMPORT_ALIAS -> {
                    val importAlias = convertImportAlias(child)
                    if (importAlias != null) {
                        aliasName = importAlias.first
                        aliasSource = importAlias.second
                    }
                }
            }
        }

        return buildImport {
            source = importDirective.toFirSourceElement()
            this.importedFqName = importedFqName
            this.isAllUnder = isAllUnder
            this.aliasName = aliasName?.let { Name.identifier(it) }
            this.aliasSource = aliasSource
        }
    }

    private fun MutableList<String>.collectSegments(expression: Node) {
        when (expression.toTokenId()) {
            KtNodeTypeId.REFERENCE_EXPRESSION -> add(expression.getAsStringWithoutBacktick())
            KtNodeTypeId.DOT_QUALIFIED_EXPRESSION -> {
                expression.forEachChildren {
                    collectSegments(it)
                }
            }
            else -> {}
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseImportDirectives
     */
    private fun convertImportDirectives(importList: Node): List<FirImport> {
        return importList.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypeId.IMPORT_DIRECTIVE -> container += convertImportDirective(node)
            }
        }
    }

    /*****    MODIFIERS    *****/
    /**
     * Convert modifiers and collect annotations.
     *
     * To convert annotations, [ModifierList.convertAnnotationsTo] or [ModifierList.convertAnnotations] must be called inside
     * a [Context.withContainerSymbol] block.
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertModifierList(modifiers: Node, isInClass: Boolean = false): ModifierList<Node> {
        return ModifierList<Node>().also { it.consume(modifiers, isInClass) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentModifierList
     */
    private fun convertTypeArgumentModifierList(modifiers: Node): TypeProjectionModifierList<Node> {
        return TypeProjectionModifierList<Node>().also { it.consume(modifiers) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentModifierList
     */
    private fun convertTypeParameterModifiers(modifiers: Node): TypeParameterModifierList<Node> {
        return TypeParameterModifierList<Node>().also { it.consume(modifiers) }
    }

    private fun ModifierList<Node>.consume(modifierList: Node, isInClass: Boolean = false) {
        modifierList.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.ANNOTATION -> annotations += it
                KtNodeTypeId.ANNOTATION_ENTRY -> annotations += it
                KtNodeTypeId.CONTEXT_PARAMETER_LIST -> contextLists += it
                KtTokenId.IN_MODIFIER, KtTokenId.FUN_MODIFIER, KtTokenId.ERROR_MODIFIER -> addModifier(it.toTokenId(), isInClass)
                in KtTokenId.ABSTRACT_MODIFIER..KtTokenId.ACTUAL_MODIFIER -> addModifier(it.toTokenId(), isInClass)
            }
        }
    }

    /*****    ANNOTATIONS    *****/
    /**
     * Convert only annotations
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertAnnotationsOnlyTo(modifierList: Node, list: MutableList<in FirAnnotationCall>) {
        modifierList.forEachChildren { node ->
            convertAnnotationOrAnnotationEntryTo(node, list)
        }
    }

    private fun ModifierList<Node>.convertAnnotationsTo(list: MutableList<in FirAnnotationCall>) {
        for (node in annotations) {
            convertAnnotationOrAnnotationEntryTo(node, list)
        }
    }

    private fun ModifierList<Node>.convertAnnotations(): List<FirAnnotationCall> {
        return buildList { convertAnnotationsTo(this) }
    }

    private fun convertAnnotationOrAnnotationEntryTo(node: Node, list: MutableList<in FirAnnotationCall>) {
        when (node.toTokenId()) {
            KtNodeTypeId.ANNOTATION -> convertAnnotationTo(node, list)
            KtNodeTypeId.ANNOTATION_ENTRY -> list += convertAnnotationEntry(node)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseAnnotationOrList
     */
    fun convertAnnotationTo(node: Node, list: MutableList<in FirAnnotationCall>) {
        var annotationTarget: AnnotationUseSiteTarget? = null
        node.forEachChildren { child ->
            when (child.toTokenId()) {
                KtNodeTypeId.ANNOTATION_TARGET -> annotationTarget = convertAnnotationTarget(child)
                KtNodeTypeId.ANNOTATION_ENTRY -> list += convertAnnotationEntry(
                    child,
                    annotationTarget,
                    runIf(annotationTarget == ALL) {
                        ConeSimpleDiagnostic(
                            "Multiple annotation syntax with @all use-site target is forbidden",
                            DiagnosticKind.MultipleAnnotationWithAllTarget
                        )
                    }
                )
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseAnnotationTarget
     */
    private fun convertAnnotationTarget(annotationUseSiteTarget: Node): AnnotationUseSiteTarget {
        lateinit var annotationTarget: AnnotationUseSiteTarget
        annotationUseSiteTarget.forEachChildren {
            when (it.toTokenId()) {
                KtTokenId.ALL_KEYWORD -> annotationTarget = ALL
                KtTokenId.FIELD_KEYWORD -> annotationTarget = FIELD
                KtTokenId.FILE_KEYWORD -> annotationTarget = FILE
                KtTokenId.PROPERTY_KEYWORD -> annotationTarget = PROPERTY
                KtTokenId.GET_KEYWORD -> annotationTarget = PROPERTY_GETTER
                KtTokenId.SET_KEYWORD -> annotationTarget = PROPERTY_SETTER
                KtTokenId.RECEIVER_KEYWORD -> annotationTarget = RECEIVER
                KtTokenId.PARAM_KEYWORD -> annotationTarget = CONSTRUCTOR_PARAMETER
                KtTokenId.SETPARAM_KEYWORD -> annotationTarget = SETTER_PARAMETER
                KtTokenId.DELEGATE_KEYWORD -> annotationTarget = PROPERTY_DELEGATE_FIELD
            }
        }

        return annotationTarget
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseAnnotation
     * can be treated as unescapedAnnotation
     */
    fun convertAnnotationEntry(
        node: Node,
        defaultAnnotationUseSiteTarget: AnnotationUseSiteTarget? = null,
        diagnostic: ConeDiagnostic? = null,
    ): FirAnnotationCall = context.withForcedLocalContext {
        var annotationUseSiteTarget: AnnotationUseSiteTarget? = null
        lateinit var constructorCalleePair: Pair<FirTypeRef, List<FirExpression>>
        node.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.ANNOTATION_TARGET -> annotationUseSiteTarget = convertAnnotationTarget(it)
                KtNodeTypeId.CONSTRUCTOR_CALLEE -> constructorCalleePair = convertConstructorInvocation(node)
            }
        }
        val qualifier = (constructorCalleePair.first as? FirUserTypeRef)?.qualifier?.last()
        val name = qualifier?.name ?: Name.special("<no-annotation-name>")
        val theCalleeReference = buildSimpleNamedReference {
            source = node
                .getChildNodeByTokenId(KtNodeTypeId.CONSTRUCTOR_CALLEE)
                ?.getChildNodeByTokenId(KtNodeTypeId.TYPE_REFERENCE)
                ?.getChildNodeByTokenId(KtNodeTypeId.USER_TYPE)
                ?.getChildNodeByTokenId(KtNodeTypeId.REFERENCE_EXPRESSION)
                ?.toFirSourceElement()
            this.name = name
        }

        if (diagnostic == null) {
            buildAnnotationCall {
                source = node.toFirSourceElement()
                useSiteTarget = annotationUseSiteTarget ?: defaultAnnotationUseSiteTarget
                annotationTypeRef = constructorCalleePair.first
                calleeReference = theCalleeReference
                extractArgumentsFrom(constructorCalleePair.second)
                typeArguments += qualifier?.typeArgumentList?.typeArguments ?: listOf()
                containingDeclarationSymbol = context.containerSymbol
            }
        } else {
            buildErrorAnnotationCall {
                source = node.toFirSourceElement()
                useSiteTarget = annotationUseSiteTarget ?: defaultAnnotationUseSiteTarget
                annotationTypeRef = constructorCalleePair.first
                this.diagnostic = diagnostic
                calleeReference = theCalleeReference
                extractArgumentsFrom(constructorCalleePair.second)
                typeArguments += qualifier?.typeArgumentList?.typeArguments ?: listOf()
                containingDeclarationSymbol = context.containerSymbol
            }
        }
    }

    private fun Node.hasValueParameters(): Boolean {
        return getChildNodesByTokenId(KtNodeTypeId.VALUE_PARAMETER_LIST).let {
            it.isNotEmpty() && it.first().getChildNodesByTokenId(KtNodeTypeId.VALUE_PARAMETER).isNotEmpty()
        }
    }

    /*****    DECLARATIONS    *****/
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassOrObject
     */
    fun convertClass(node: Node): FirRegularClass {
        var modifiers: ModifierList<Node>? = null
        var classKind: ClassKind = ClassKind.CLASS
        var identifier: String? = null
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        var primaryConstructor: Node? = null
        val typeConstraints = mutableListOf<TypeConstraint>()
        var classBody: Node? = null
        var superTypeList: Node? = null
        var typeParameterList: Node? = null
        node.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.MODIFIER_LIST -> modifiers = convertModifierList(it, isInClass = true)
                KtTokenId.IDENTIFIER -> identifier = it.asText
            }
        }

        val calculatedModifiers = modifiers ?: ModifierList()
        val className = identifier.nameAsSafeName(if (calculatedModifiers.isCompanion()) "Companion" else "")
        val isLocalWithinParent = node.getParent()?.toTokenId() != KtNodeTypeId.CLASS_BODY && isClassLocal(node) { getParent() }
        val classIsExpect = calculatedModifiers.hasExpect() || context.containerIsExpect

        return context.withChildClassName(className, isExpect = classIsExpect, isLocalWithinParent) {
            val classSymbol = FirRegularClassSymbol(context.currentClassId)
            context.withContainerSymbol(classSymbol) {
                node.forEachChildren {
                    when (it.toTokenId()) {
                        KtTokenId.CLASS_KEYWORD -> classKind = ClassKind.CLASS
                        KtTokenId.INTERFACE_KEYWORD -> classKind = ClassKind.INTERFACE
                        KtTokenId.OBJECT_KEYWORD -> classKind = ClassKind.OBJECT
                        KtNodeTypeId.TYPE_PARAMETER_LIST -> typeParameterList = it
                        KtNodeTypeId.PRIMARY_CONSTRUCTOR -> primaryConstructor = it
                        KtNodeTypeId.SUPER_TYPE_LIST -> superTypeList = it
                        KtNodeTypeId.TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                        KtNodeTypeId.CLASS_BODY -> classBody = it
                    }
                }

                if (classKind == ClassKind.CLASS) {
                    classKind = when {
                        calculatedModifiers.isEnum() -> ClassKind.ENUM_CLASS
                        calculatedModifiers.isAnnotation() -> ClassKind.ANNOTATION_CLASS
                        else -> classKind
                    }
                }

                val isLocal = context.inLocalContext
                val status = FirDeclarationStatusImpl(
                    if (isLocal) Visibilities.Local else calculatedModifiers.getVisibility(publicByDefault = true),
                    calculatedModifiers.getModality(isClassOrObject = true)
                ).apply {
                    isExpect = classIsExpect
                    isActual = calculatedModifiers.hasActual()
                    isInner = calculatedModifiers.isInner()
                    isCompanion = calculatedModifiers.isCompanion() && classKind == ClassKind.OBJECT
                    isData = calculatedModifiers.isDataClass()
                    isInline = calculatedModifiers.isInlineClass()
                    isValue = calculatedModifiers.isValueClass() || calculatedModifiers.isInlineClass()
                    isFun = calculatedModifiers.isFunctionalInterface()
                    isRichError = calculatedModifiers.isErrorClass()
                    isExternal = calculatedModifiers.hasExternal()
                }


                typeParameterList?.let { firTypeParameters += convertTypeParameters(it, typeConstraints, classSymbol) }

                context.withCapturedTypeParameters(
                    status = status.isInner || isLocal,
                    declarationSource = node.toFirSourceElement(),
                    currentFirTypeParameters = firTypeParameters,
                ) {
                    var delegatedFieldsMap: Map<Int, FirFieldSymbol>? = null
                    val companionBlockCollector = CompanionBlockCollector()
                    buildRegularClass {
                        source = node.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        name = className
                        this.status = status
                        this.classKind = classKind
                        scopeProvider = baseScopeProvider
                        symbol = classSymbol
                        modifiers?.convertAnnotationsTo(annotations)
                        typeParameters += firTypeParameters

                        context.appendOuterTypeParameters(ignoreLastLevel = true, typeParameters)

                        val selfType = node.toDelegatedSelfType(this)
                        registerSelfType(selfType)

                        val delegationSpecifiers = superTypeList?.let { convertDelegationSpecifiers(it) }
                        var delegatedSuperTypeRef: FirTypeRef? = delegationSpecifiers?.superTypeCalls?.lastOrNull()?.delegatedSuperTypeRef
                        val delegatedConstructorSource: KtLightSourceElement? = delegationSpecifiers?.superTypeCalls?.lastOrNull()?.source

                        val superTypeRefs = mutableListOf<FirTypeRef>()

                        delegationSpecifiers?.let { superTypeRefs += it.superTypesRef }

                        when {
                            calculatedModifiers.isEnum() && (classKind == ClassKind.ENUM_CLASS) && delegatedConstructorSource == null -> {
                                delegatedSuperTypeRef = buildResolvedTypeRef {
                                    coneType = ConeClassLikeTypeImpl(
                                        implicitEnumType.coneType.lookupTag,
                                        arrayOf(selfType.coneType),
                                        isMarkedNullable = false
                                    )
                                    source = node.toFirSourceElement(KtFakeSourceElementKind.EnumSuperTypeRef)
                                }
                                superTypeRefs += delegatedSuperTypeRef
                            }
                            calculatedModifiers.isAnnotation() && (classKind == ClassKind.ANNOTATION_CLASS) -> {
                                superTypeRefs += implicitAnnotationType
                                // A written super call is kept for red code to be resolved as written
                                delegatedSuperTypeRef = delegatedSuperTypeRef ?: implicitAnyType
                            }
                        }

                        val classIsKotlinAny = symbol.classId == StandardClassIds.Any

                        if (superTypeRefs.isEmpty() && !classIsKotlinAny) {
                            val classIsKotlinNothing = symbol.classId == StandardClassIds.Nothing
                            val defaultSuperTypeRef = if (calculatedModifiers.isErrorClass()) implicitRichErrorType else implicitAnyType
                            // kotlin.Nothing doesn't have `Any` supertype, but does have delegating constructor call to Any
                            if (!classIsKotlinNothing) {
                                superTypeRefs += defaultSuperTypeRef
                            }
                            delegatedSuperTypeRef = defaultSuperTypeRef
                        }

                        this.superTypeRefs += superTypeRefs

                        val secondaryConstructors = classBody.getChildNodesByTokenId(KtNodeTypeId.SECONDARY_CONSTRUCTOR)
                        val classWrapper = ClassWrapper(
                            calculatedModifiers, classKind, this, hasSecondaryConstructor = secondaryConstructors.isNotEmpty(),
                            hasDefaultConstructor = if (primaryConstructor != null) !primaryConstructor.hasValueParameters()
                            else secondaryConstructors.isEmpty() || secondaryConstructors.any { !it.hasValueParameters() },
                            delegatedSelfTypeRef = selfType,
                            delegatedSuperTypeRef = delegatedSuperTypeRef ?: FirImplicitTypeRefImplWithoutSource,
                            delegatedSuperCalls = delegationSpecifiers?.superTypeCalls ?: emptyList(),
                            companionBlockCollector,
                        )
                        //parse primary constructor
                        val primaryConstructorWrapper = convertPrimaryConstructor(
                            primaryConstructor,
                            selfType.source,
                            classWrapper,
                            delegatedConstructorSource,
                            containingClassIsExpectClass = status.isExpect,
                            isImplicitlyActual = isImplicitlyActual(status, classKind),
                            isKotlinAny = classIsKotlinAny,
                        )
                        val firPrimaryConstructor = primaryConstructorWrapper?.firConstructor
                        firPrimaryConstructor?.let { declarations += it }
                        delegationSpecifiers?.delegateFieldsMap?.values?.mapTo(declarations) { it.fir }
                        delegatedFieldsMap = delegationSpecifiers?.delegateFieldsMap?.takeIf { it.isNotEmpty() }

                        val properties = mutableListOf<FirProperty>()
                        if (primaryConstructor != null && firPrimaryConstructor != null) {
                            //parse properties
                            properties += primaryConstructorWrapper.valueParameters
                                .filter { it.hasValOrVar() }
                                .map {
                                    it.toFirPropertyFromPrimaryConstructor(
                                        baseModuleData,
                                        callableIdForName(it.firValueParameter.name),
                                        classIsExpect,
                                        currentDispatchReceiverType(context),
                                        context
                                    )
                                }
                            addDeclarations(properties)
                        }

                        //parse declarations
                        classBody?.let {
                            addDeclarations(convertClassBody(it, classWrapper))
                        }

                        //parse data class
                        if (calculatedModifiers.isDataClass() && firPrimaryConstructor != null) {
                            val zippedParameters = properties.map { it.source!!.toNode() to it }
                            DataClassMembersGenerator(
                                primaryConstructor ?: node,
                                this,
                                firPrimaryConstructor,
                                zippedParameters,
                                context.packageFqName,
                                context.className,
                                addValueParameterAnnotations = { valueParam ->
                                    context.withContainerSymbol(symbol) {
                                        valueParam.forEachChildren { node ->
                                            if (node.toTokenId() == KtNodeTypeId.MODIFIER_LIST) {
                                                buildList {
                                                    convertAnnotationsOnlyTo(node, this)
                                                }.filterTo(annotations) {
                                                    it.useSiteTarget.appliesToPrimaryConstructorParameter()
                                                }
                                            }
                                        }
                                    }
                                }
                            ).generate()
                        }

                        if (calculatedModifiers.isEnum()) {
                            generateValuesFunction(
                                baseModuleData,
                                context.packageFqName,
                                context.className,
                                classIsExpect
                            )
                            generateValueOfFunction(
                                baseModuleData,
                                context.packageFqName,
                                context.className,
                                classIsExpect
                            )
                            generateEntriesGetter(
                                baseModuleData,
                                context.packageFqName,
                                context.className,
                                classIsExpect
                            )
                        }
                        initCompanionObjectSymbolAttr()

                        contextParameters.addContextParameters(modifiers?.contextLists, classSymbol)
                    }.apply {
                        this.delegateFieldsMap = delegatedFieldsMap
                        companionBlockCollector.toCompanionBlockInfoOrNull()?.let { companionBlocks = it }
                    }
                }.also {
                    fillDanglingConstraintsTo(firTypeParameters, typeConstraints, it)
                }
            }
        }.also {
            if (node.getParent()?.toTokenId() == KtNodeTypeId.CLASS_BODY) {
                it.initContainingClassForLocalAttr()
            }
            it.initContainingScriptOrReplAttr()
            if (isDirectlyInsideCompanionBlock) {
                it.isIllegalCompanionBlockMember = true
            }
        }
    }

    private inline fun isClassLocal(classNode: Node, getParent: Node.() -> Node?): Boolean {
        if (classNode.getParent()?.getParent()?.toTokenId() == KtNodeTypeId.SCRIPT) return false
        var currentNode: Node? = classNode
        while (currentNode != null) {
            val tokenType = currentNode.toTokenId()
            val parent = currentNode.getParent()
            val parentTokenType = parent?.toTokenId()
            if (tokenType == KtNodeTypeId.PROPERTY || tokenType == KtNodeTypeId.FUNCTION) {
                val grandParent = parent?.getParent()
                when (parentTokenType) {
                    KtNodeTypeId.FILE -> return true
                    KtNodeTypeId.CLASS_BODY if !(grandParent?.toTokenId() == KtNodeTypeId.OBJECT_DECLARATION && grandParent.getParent()?.toTokenId() == KtNodeTypeId.OBJECT_LITERAL)
                        -> return true
                    KtNodeTypeId.BLOCK if grandParent?.toTokenId() == KtNodeTypeId.SCRIPT -> return true
                }
            }
            // NB: enum entry nested classes are considered local by FIR design (see discussion in KT-45115)
            if (parentTokenType == KtNodeTypeId.ENUM_ENTRY) {
                return true
            }
            if (tokenType == KtNodeTypeId.BLOCK) {
                return true
            }
            currentNode = parent
        }
        return false
    }

    /**
     * see PsiRawFirBuilder.Visitor.visitObjectLiteralExpression
     *
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseObjectLiteral
     */
    fun convertObjectLiteral(node: Node): FirAnonymousObjectExpression {
        return context.withChildClassName(SpecialNames.ANONYMOUS, forceLocalContext = true, isExpect = false) {
            var delegatedFieldsMap: Map<Int, FirFieldSymbol>? = null
            val objectDeclaration = node.getChildNodesByTokenId(KtNodeTypeId.OBJECT_DECLARATION).first()
            val objectSource = objectDeclaration.toFirSourceElement()
            val anonymousObjectSymbol = FirAnonymousObjectSymbol(context.packageFqName)
            // Type parameters are prohibited for objects, but they are kept for the FIR checker and the Analysis API
            val firTypeParameters = objectDeclaration.getChildNodeByTokenId(KtNodeTypeId.TYPE_PARAMETER_LIST)
                ?.let { convertTypeParameters(it, emptyList(), anonymousObjectSymbol) }
                .orEmpty()
            context.withCapturedTypeParameters(status = true, objectSource, firTypeParameters) {
                buildAnonymousObjectExpression {
                    source = node.toFirSourceElement()

                    var modifiers: ModifierList<Node>? = null
                    var primaryConstructor: Node? = null
                    val superTypeRefs = mutableListOf<FirTypeRef>()
                    var delegatedSuperTypeRef: FirTypeRef? = null
                    var classBody: Node? = null
                    var delegatedConstructorSource: KtLightSourceElement? = null
                    var delegatedSuperCalls: List<DelegatedConstructorWrapper>? = null
                    var delegateFields: List<FirField>? = null

                    objectDeclaration.forEachChildren { child ->
                        when (child.toTokenId()) {
                            KtNodeTypeId.MODIFIER_LIST -> {
                                modifiers = convertModifierList(child)
                            }
                            KtNodeTypeId.PRIMARY_CONSTRUCTOR -> primaryConstructor = child
                            KtNodeTypeId.SUPER_TYPE_LIST -> convertDelegationSpecifiers(child).let { specifiers ->
                                delegatedSuperTypeRef = specifiers.superTypeCalls.lastOrNull()?.delegatedSuperTypeRef
                                superTypeRefs += specifiers.superTypesRef
                                delegatedConstructorSource = specifiers.superTypeCalls.lastOrNull()?.source
                                delegateFields = specifiers.delegateFieldsMap.values.map { it.fir }
                                delegatedFieldsMap = specifiers.delegateFieldsMap.takeIf { it.isNotEmpty() }
                                delegatedSuperCalls = specifiers.superTypeCalls
                            }
                            KtNodeTypeId.CLASS_BODY -> classBody = child
                        }
                    }
                    val companionBlockCollector = CompanionBlockCollector()
                    anonymousObject = buildAnonymousObject {
                        source = objectSource
                        origin = FirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        classKind = ClassKind.CLASS
                        scopeProvider = baseScopeProvider
                        symbol = anonymousObjectSymbol
                        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                        typeParameters += firTypeParameters
                        context.appendOuterTypeParameters(ignoreLastLevel = true, typeParameters)
                        val delegatedSelfType = objectDeclaration.toDelegatedSelfType(this)
                        registerSelfType(delegatedSelfType)

                        if (superTypeRefs.isEmpty()) {
                            superTypeRefs += implicitAnyType
                            delegatedSuperTypeRef = implicitAnyType
                        }
                        val delegatedSuperType = delegatedSuperTypeRef ?: FirImplicitTypeRefImplWithoutSource

                        modifiers?.convertAnnotationsTo(annotations)
                        this.superTypeRefs += superTypeRefs

                        val classWrapper = ClassWrapper(
                            modifiers ?: ModifierList(),
                            ClassKind.OBJECT,
                            this,
                            hasSecondaryConstructor = classBody.getChildNodesByTokenId(KtNodeTypeId.SECONDARY_CONSTRUCTOR).isNotEmpty(),
                            hasDefaultConstructor = false,
                            delegatedSelfTypeRef = delegatedSelfType,
                            delegatedSuperTypeRef = delegatedSuperType,
                            delegatedSuperCalls = delegatedSuperCalls ?: emptyList(),
                            companionBlockCollector,
                        )
                        //parse primary constructor
                        convertPrimaryConstructor(
                            primaryConstructor,
                            delegatedSelfType.source,
                            classWrapper,
                            delegatedConstructorSource,
                            containingClassIsExpectClass = false
                        )?.let { this.declarations += it.firConstructor }
                        delegateFields?.let { this.declarations += it }

                        //parse declarations
                        classBody?.let {
                            this.declarations += convertClassBody(it, classWrapper)
                        }
                    }.apply {
                        this.delegateFieldsMap = delegatedFieldsMap
                        companionBlockCollector.toCompanionBlockInfoOrNull()?.let { companionBlocks = it }
                    }
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumEntry
     */
    private fun convertEnumEntry(enumEntry: Node, classWrapper: ClassWrapper<Node>): FirEnumEntry {
        var modifiers: ModifierList<Node>? = null
        lateinit var identifier: String
        val enumSuperTypeCallEntry = mutableListOf<FirExpression>()
        var classBodyNode: Node? = null
        var superTypeCallEntry: Node? = null
        enumEntry.getChildNodeByTokenId(KtTokenId.IDENTIFIER)?.let {
            identifier = it.asText
        }

        val enumEntryName = identifier.nameAsSafeName()
        val containingClassIsExpectClass = classWrapper.hasExpect() || context.containerIsExpect
        return buildEnumEntry {
            symbol = FirEnumEntrySymbol(CallableId(context.currentClassId, enumEntryName))
            context.withContainerSymbol(symbol) {
                enumEntry.forEachChildren {
                    when (it.toTokenId()) {
                        KtNodeTypeId.MODIFIER_LIST -> {
                            modifiers = convertModifierList(it)
                        }
                        KtNodeTypeId.INITIALIZER_LIST -> {
                            enumSuperTypeCallEntry += convertInitializerList(it)
                            it.getChildNodeByTokenId(KtNodeTypeId.SUPER_TYPE_CALL_ENTRY)?.let { superTypeCall ->
                                superTypeCallEntry = superTypeCall
                            }
                        }
                        KtNodeTypeId.CLASS_BODY -> classBodyNode = it
                    }
                }

                source = enumEntry.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = classWrapper.delegatedSelfTypeRef
                name = enumEntryName
                status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                    isStatic = true
                    isExpect = containingClassIsExpectClass
                }
                isLocal = context.inLocalContext
                if (classWrapper.hasDefaultConstructor && enumEntry.getChildNodeByTokenId(KtNodeTypeId.INITIALIZER_LIST) == null &&
                    modifiers.let { it == null || it.annotations.isEmpty() } && classBodyNode == null
                ) {
                    return@buildEnumEntry
                }
                modifiers?.convertAnnotationsTo(annotations)
                initializer = context.withChildClassName(enumEntryName, isExpect = false) {
                    buildAnonymousObjectExpression {
                        val entrySource = enumEntry.toFirSourceElement(KtFakeSourceElementKind.EnumInitializer)
                        source = entrySource
                        val companionBlockCollector = CompanionBlockCollector()
                        anonymousObject = buildAnonymousObject {
                            source = entrySource
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            classKind = ClassKind.ENUM_ENTRY
                            scopeProvider = baseScopeProvider
                            symbol = FirAnonymousObjectSymbol(context.packageFqName)
                            status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                            val enumClassWrapper = ClassWrapper(
                                modifiers ?: ModifierList(),
                                ClassKind.ENUM_ENTRY,
                                this,
                                hasSecondaryConstructor = classBodyNode.getChildNodesByTokenId(KtNodeTypeId.SECONDARY_CONSTRUCTOR).isNotEmpty(),
                                hasDefaultConstructor = false,
                                delegatedSelfTypeRef = buildResolvedTypeRef {
                                    coneType = ConeClassLikeTypeImpl(
                                        this@buildAnonymousObject.symbol.toLookupTag(),
                                        ConeTypeProjection.EMPTY_ARRAY,
                                        isMarkedNullable = false
                                    )
                                    source = enumEntry.toFirSourceElement(KtFakeSourceElementKind.ClassSelfTypeRef)
                                }.also { registerSelfType(it) },
                                delegatedSuperTypeRef = classWrapper.delegatedSelfTypeRef,
                                delegatedSuperCalls = listOf(
                                    DelegatedConstructorWrapper(
                                        classWrapper.delegatedSelfTypeRef,
                                        enumSuperTypeCallEntry,
                                        superTypeCallEntry?.toFirSourceElement() as KtLightSourceElement?,
                                    )
                                ),
                                companionBlockCollector,
                            )
                            superTypeRefs += enumClassWrapper.delegatedSuperTypeRef
                            convertPrimaryConstructor(
                                null,
                                enumEntry.toFirSourceElement(),
                                enumClassWrapper,
                                superTypeCallEntry?.toFirSourceElement() as KtLightSourceElement?,
                                isEnumEntry = true,
                                containingClassIsExpectClass = containingClassIsExpectClass
                            )?.let { declarations += it.firConstructor }
                            classBodyNode?.also {
                                // Use ANONYMOUS_OBJECT_NAME for the owner class id of enum entry declarations
                                context.withChildClassName(SpecialNames.ANONYMOUS, forceLocalContext = true, isExpect = false) {
                                    declarations += convertClassBody(it, enumClassWrapper)
                                }
                            }
                        }.apply {
                            companionBlockCollector.toCompanionBlockInfoOrNull()?.let { companionBlocks = it }
                        }
                    }
                }
            }
        }.also {
            it.containingClassForStaticMemberAttr = currentDispatchReceiverType(context)!!.lookupTag
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumEntry
     */
    private fun convertInitializerList(initializerList: Node): List<FirExpression> {
        val firValueArguments = mutableListOf<FirExpression>()
        initializerList.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.SUPER_TYPE_CALL_ENTRY -> convertConstructorInvocation(it).apply {
                    firValueArguments += second
                }
            }
        }

        return firValueArguments
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassBody
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumClassBody
     */
    private fun convertClassBody(classBody: Node, classWrapper: ClassWrapper<Node>?): List<FirDeclaration> {
        val modifierLists = mutableListOf<Node>()
        val firDeclarations = classBody.forEachChildrenReturnList { node, container ->
            convertDeclarationFromClassBody(node, container, classWrapper, modifierLists)
        }

        convertDanglingModifierListsInClassBody(modifierLists, firDeclarations)
        return firDeclarations
    }

    private fun convertDeclarationFromClassBody(
        node: Node,
        container: MutableList<FirDeclaration>,
        classWrapper: ClassWrapper<Node>?,
        modifierLists: MutableList<Node>,
    ) {
        when (node.toTokenId()) {
            KtNodeTypeId.ENUM_ENTRY -> container += convertEnumEntry(node, classWrapper!!)
            KtNodeTypeId.CLASS, KtNodeTypeId.OBJECT_DECLARATION -> container += convertClass(node)
            KtNodeTypeId.FUNCTION -> container += convertFunctionDeclaration(node) as FirDeclaration
            KtNodeTypeId.PROPERTY -> container += convertPropertyDeclaration(node, classWrapper)
            KtNodeTypeId.TYPEALIAS -> container += convertTypeAlias(node)
            KtNodeTypeId.CLASS_INITIALIZER -> convertAnonymousInitializer(
                node, classWrapper!!.classBuilder.ownerRegularOrAnonymousObjectSymbol
            )?.also {
                container += it
            } //anonymousInitializer
            KtNodeTypeId.SECONDARY_CONSTRUCTOR -> container += convertSecondaryConstructor(node, classWrapper!!)
            KtNodeTypeId.MODIFIER_LIST -> modifierLists += node
            KtNodeTypeId.DESTRUCTURING_DECLARATION -> addNonLocalDestructuringDeclaration(container, node)
            KtNodeTypeId.COMPANION_BLOCK -> {
                classWrapper?.companionBlockCollector?.collect(node.toFirSourceElement(), isNested = isDirectlyInsideCompanionBlock)
                context.withCompanionBlock {
                    node.getChildNodeByTokenId(KtNodeTypeId.CLASS_BODY)?.let { container.addAll(convertClassBody(it, classWrapper)) }
                }
            }
        }
    }

    private fun convertDanglingModifierListsInClassBody(
        modifierLists: MutableList<Node>,
        firDeclarations: MutableList<FirDeclaration>,
    ) {
        for (node in modifierLists) {
            firDeclarations += buildErrorNonLocalDeclarationForDanglingModifierList(node)
        }
    }

    private fun addNonLocalDestructuringDeclaration(destination: MutableList<FirDeclaration>, node: Node) {
        val symbol = createNonLocalDestructuringDeclarationContainerSymbol()

        // The annotations are converted later along with the container, and the entries' ones are copied to the entries
        val destructuringDeclaration = context.withContainerSymbol(symbol) {
            convertDestructingDeclaration(node, convertAnnotations = false)
        }

        val container = buildNonLocalDestructuringDeclarationContainer(
            destructuringDeclaration.source,
            symbol,
            destructuringDeclaration.initializer,
            baseModuleData,
        ) { annotationContainer ->
            node.getChildNodeByTokenId(KtNodeTypeId.MODIFIER_LIST)?.let {
                convertAnnotationsOnlyTo(it, annotationContainer.annotations)
            }
        }

        addDestructuringStatements(
            destination,
            context,
            baseModuleData,
            destructuringDeclaration,
            container,
            isTmpVariable = true,
            forceLocal = false,
            isMember = true,
            isCompanionBlockMember = isDirectlyInsideCompanionBlock,
        ) {
            configureNonLocalDestructuringDeclarationEntry(it, container)
        }
    }

    private fun buildErrorNonLocalDeclarationForDanglingModifierList(node: Node): FirDanglingModifierList {
        return buildDanglingModifierList {
            this.source = node.toFirSourceElement(KtFakeSourceElementKind.DanglingModifierList)
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            diagnostic = ConeDanglingModifierOnTopLevel
            symbol = FirDanglingModifierSymbol()
            context.withContainerSymbol(symbol) {
                val modifiers = convertModifierList(node)
                contextParameters.addContextParameters(modifiers.contextLists, symbol)
                modifiers.convertAnnotationsTo(annotations)
            }
        }.apply {
            containingClassAttr = currentDispatchReceiverType(context)?.lookupTag
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassOrObject
     * primaryConstructor branch
     */
    private fun convertPrimaryConstructor(
        primaryConstructor: Node?,
        selfTypeSource: KtSourceElement?,
        classWrapper: ClassWrapper<Node>,
        delegatedConstructorSource: KtLightSourceElement?,
        isEnumEntry: Boolean = false,
        containingClassIsExpectClass: Boolean,
        isImplicitlyActual: Boolean = false,
        isKotlinAny: Boolean = false,
    ): PrimaryConstructor<Node>? {
        val shouldGenerateImplicitConstructor =
            (classWrapper.isEnumEntry() || !classWrapper.hasSecondaryConstructor) &&
                    !classWrapper.isInterface() &&
                    (!containingClassIsExpectClass || classWrapper.classBuilder.classKind == ClassKind.ENUM_ENTRY)
        val isErrorConstructor = primaryConstructor == null && !shouldGenerateImplicitConstructor
        if (isErrorConstructor && classWrapper.delegatedSuperCalls.isEmpty()) {
            return null
        }

        val constructorSymbol = FirConstructorSymbol(callableIdForClassConstructor())
        context.withContainerSymbol(constructorSymbol) {
            var modifiersIfPresent: ModifierList<Node>? = null
            val valueParameters = mutableListOf<ValueParameter<Node>>()
            primaryConstructor?.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypeId.MODIFIER_LIST -> {
                        modifiersIfPresent = convertModifierList(it)
                    }
                    KtNodeTypeId.VALUE_PARAMETER_LIST -> valueParameters += convertValueParameters(
                        it,
                        constructorSymbol,
                        ValueParameterDeclaration.PRIMARY_CONSTRUCTOR
                    )
                }
            }

            val modifiers = modifiersIfPresent ?: ModifierList()

            val generateDelegatedSuperCall = shouldGenerateDelegatedSuperCall(
                isAnySuperCall = isKotlinAny,
                isExpectClass = containingClassIsExpectClass,
                isEnumEntry = isEnumEntry,
                hasExplicitDelegatedCalls = classWrapper.delegatedSuperCalls.isNotEmpty()
            )

            val generateDelegatedConstructorCall = !headerMode || context.forceKeepingTheBodyInHeaderMode
            val firDelegatedCall = runIf(generateDelegatedSuperCall && generateDelegatedConstructorCall) {
                fun createDelegatedConstructorCall(
                    delegatedConstructorSource: KtLightSourceElement?,
                    delegatedSuperTypeRef: FirTypeRef,
                    arguments: List<FirExpression>,
                ): FirDelegatedConstructorCall {
                    return buildDelegatedConstructorCall {
                        source = delegatedConstructorSource
                            ?: primaryConstructor?.toFirSourceElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                                    ?: selfTypeSource?.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                        constructedTypeRef = delegatedSuperTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
                        isThis = false
                        calleeReference = buildExplicitSuperReference {
                            //[dirty] in the case of enum classWrapper.delegatedSuperTypeRef.source is the whole enum source
                            source = if (!isEnumEntry) {
                                classWrapper.delegatedSuperTypeRef.source?.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                                    ?: this@buildDelegatedConstructorCall.source?.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                            } else {
                                delegatedConstructorSource
                                    ?.toNode()
                                    ?.getChildNodeByTokenId(KtNodeTypeId.CONSTRUCTOR_CALLEE)
                                    ?.toFirSourceElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                                    ?: this@buildDelegatedConstructorCall.source
                            }

                            superTypeRef = this@buildDelegatedConstructorCall.constructedTypeRef
                        }
                        extractArgumentsFrom(arguments)
                    }
                }
                if (classWrapper.delegatedSuperCalls.size <= 1) {
                    createDelegatedConstructorCall(
                        delegatedConstructorSource,
                        classWrapper.delegatedSuperTypeRef,
                        classWrapper.delegatedSuperCalls.lastOrNull()?.arguments ?: emptyList(),
                    )
                } else {
                    buildMultiDelegatedConstructorCall {
                        classWrapper.delegatedSuperCalls.mapTo(delegatedConstructorCalls) { (delegatedSuperTypeRef, arguments, source) ->
                            createDelegatedConstructorCall(source, delegatedSuperTypeRef, arguments)
                        }
                    }
                }
            }

            val explicitVisibility = runIf(primaryConstructor != null) {
                modifiers.getVisibility().takeUnless { it == Visibilities.Unknown }
            }
            val status = FirDeclarationStatusImpl(explicitVisibility ?: classWrapper.defaultConstructorVisibility(), Modality.FINAL).apply {
                isExpect = modifiers.hasExpect() || context.containerIsExpect
                isActual = modifiers.hasActual() || isImplicitlyActual
                isInner = classWrapper.isInner()
                isFromSealedClass = classWrapper.isSealed() && explicitVisibility !== Visibilities.Private
                isFromEnumClass = classWrapper.isEnum()
            }

            val builder = when {
                isErrorConstructor -> createErrorConstructorBuilder(ConeNoConstructorError)
                else -> FirPrimaryConstructorBuilder()
            }
            builder.apply {
                source = primaryConstructor?.toFirSourceElement()
                    ?: selfTypeSource?.fakeElement(KtFakeSourceElementKind.ImplicitConstructor)
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = classWrapper.delegatedSelfTypeRef
                dispatchReceiverType = classWrapper.obtainDispatchReceiverForConstructor()
                this.status = status
                isLocal = context.inLocalContext
                symbol = constructorSymbol
                modifiersIfPresent?.convertAnnotationsTo(annotations)
                typeParameters += constructorTypeParametersFromConstructedClass(classWrapper.classBuilder.typeParameters)
                this.valueParameters += valueParameters.map { it.firValueParameter }
                delegatedConstructor = firDelegatedCall
                this.body = null
                contextParameters.addContextParameters(modifiers.contextLists, constructorSymbol)
            }

            return PrimaryConstructor(
                builder.build().apply {
                    containingClassForStaticMemberAttr = currentDispatchReceiverType(context)!!.lookupTag
                },
                valueParameters,
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseMemberDeclarationRest
     * at INIT keyword
     *
     * @param containingDeclarationSymbol containing declaration symbol, if any
     * @param isLocal if `true`, the initializer is not used as a containing declaration for the contents of the initializer
     */
    private fun convertAnonymousInitializer(
        anonymousInitializer: Node,
        containingDeclarationSymbol: FirBasedSymbol<*>,
        isLocal: Boolean = false,
    ): FirAnonymousInitializer? {
        if (headerMode && !context.forceKeepingTheBodyInHeaderMode) {
            return null
        }
        return createAnonymousInitializer(anonymousInitializer, containingDeclarationSymbol, isLocal) { annotations ->
            var firBlock: FirBlock? = null
            anonymousInitializer.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypeId.MODIFIER_LIST -> convertAnnotationsOnlyTo(it, annotations)
                    KtNodeTypeId.BLOCK -> context.withForcedLocalContext {
                        firBlock = convertBlock(it)
                    }
                }
            }
            firBlock
        }
    }

    private fun convertScriptInitializer(
        scriptInitializer: Node,
        containingDeclarationSymbol: FirBasedSymbol<*>,
        isLocal: Boolean = false,
    ): FirAnonymousInitializer {
        return createAnonymousInitializer(scriptInitializer, containingDeclarationSymbol, isLocal) {
            convertBlockExpressionWithoutBuilding(scriptInitializer).build()
        }
    }

    private inline fun createAnonymousInitializer(
        anonymousInitializer: Node,
        containingDeclarationSymbol: FirBasedSymbol<*>,
        isLocal: Boolean,
        buildBlock: (MutableList<FirAnnotation>) -> FirBlock?
    ): FirAnonymousInitializer {
        val initializerSymbol = FirAnonymousInitializerSymbol()
        return context.withContainerSymbol(initializerSymbol, isLocal) {
            buildAnonymousInitializer {
                symbol = initializerSymbol
                source = anonymousInitializer.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                body = context.withForcedLocalContext {
                    buildBlock(annotations) ?: buildEmptyExpressionBlock()
                }
                this.containingDeclarationSymbol = containingDeclarationSymbol
            }
        }.apply {
            if (isDirectlyInsideCompanionBlock) {
                isIllegalCompanionBlockMember = true
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseSecondaryConstructor
     */
    private fun convertSecondaryConstructor(secondaryConstructor: Node, classWrapper: ClassWrapper<Node>): FirConstructor {
        var modifiers: ModifierList<Node>? = null
        val firValueParameters = mutableListOf<ValueParameter<Node>>()
        var constructorDelegationCall: FirDelegatedConstructorCall? = null
        var block: Node? = null

        val constructorSymbol = FirConstructorSymbol(callableIdForClassConstructor())
        return context.withContainerSymbol(constructorSymbol) {
            var delegatedConstructorNode: Node? = null
            secondaryConstructor.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypeId.MODIFIER_LIST -> {
                        modifiers = convertModifierList(it)
                    }
                    KtNodeTypeId.VALUE_PARAMETER_LIST -> firValueParameters += convertValueParameters(
                        it,
                        constructorSymbol,
                        ValueParameterDeclaration.FUNCTION
                    )
                    KtNodeTypeId.CONSTRUCTOR_DELEGATION_CALL -> delegatedConstructorNode = it
                    KtNodeTypeId.BLOCK -> block = it
                }
            }

            val delegatedSelfTypeRef = classWrapper.delegatedSelfTypeRef
            val calculatedModifiers = modifiers ?: ModifierList()
            val isExpect = calculatedModifiers.hasExpect() || context.containerIsExpect
            val generateDelegatedConstructorCall = !headerMode || context.forceKeepingTheBodyInHeaderMode
            if (delegatedConstructorNode != null && generateDelegatedConstructorCall) {
                constructorDelegationCall = convertConstructorDelegationCall(delegatedConstructorNode, classWrapper, isExpect)
            }

            val explicitVisibility = calculatedModifiers.getVisibility().takeUnless { it == Visibilities.Unknown }
            val status = FirDeclarationStatusImpl(explicitVisibility ?: classWrapper.defaultConstructorVisibility(), Modality.FINAL).apply {
                this.isExpect = isExpect
                isActual = calculatedModifiers.hasActual()
                isInner = classWrapper.isInner()
                isFromSealedClass = classWrapper.isSealed() && explicitVisibility !== Visibilities.Private
                isFromEnumClass = classWrapper.isEnum()
            }

            val target = FirFunctionTarget(labelName = null, isLambda = false)
            buildConstructor {
                source = secondaryConstructor.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = delegatedSelfTypeRef
                dispatchReceiverType = classWrapper.obtainDispatchReceiverForConstructor()
                this.status = status
                isLocal = context.inLocalContext
                symbol = constructorSymbol
                delegatedConstructor = constructorDelegationCall

                context.firFunctionTargets += target
                modifiers?.convertAnnotationsTo(annotations)
                typeParameters += constructorTypeParametersFromConstructedClass(classWrapper.classBuilder.typeParameters)
                valueParameters += firValueParameters.map { it.firValueParameter }
                val [body, contractDescription] = context.withForcedLocalContext {
                    convertFunctionBody(block, null, allowLegacyContractDescription = true)
                }
                this.body = body?.takeIf { it.statements.isNotEmpty() }
                contractDescription?.let { this.contractDescription = it }
                context.firFunctionTargets.removeLast()
                this.contextParameters.addContextParameters(modifiers?.contextLists, constructorSymbol)
            }.also {
                it.containingClassForStaticMemberAttr = currentDispatchReceiverType(context)!!.lookupTag
                target.bind(it)
            }
        }.apply {
            if (isDirectlyInsideCompanionBlock) {
                isIllegalCompanionBlockMember = true
            }
        }
    }

    private fun ClassWrapper<Node>.obtainDispatchReceiverForConstructor(): ConeClassLikeType? =
        if (isInner()) dispatchReceiverForInnerClassConstructor() else null

    /**
     * see PsiRawFirBuilder.Visitor.convert(KtConstructorDelegationCall, FirTypeRef, Boolean)
     */
    private fun convertConstructorDelegationCall(
        constructorDelegationCall: Node,
        classWrapper: ClassWrapper<Node>,
        isExpect: Boolean,
    ): FirDelegatedConstructorCall? {
        var thisKeywordPresent = false
        val firValueArguments = mutableListOf<FirExpression>()
        constructorDelegationCall.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.CONSTRUCTOR_DELEGATION_REFERENCE -> if (it.asText == "this") thisKeywordPresent = true
                KtNodeTypeId.VALUE_ARGUMENT_LIST -> firValueArguments += expressionConverter.convertValueArguments(it)
            }
        }

        val isImplicit = constructorDelegationCall.asText.isEmpty()
        if (isImplicit && (classWrapper.modifiers.hasExternal() || isExpect)) {
            return null
        }
        val isThis = thisKeywordPresent
        val delegatedType =
            when {
                isThis -> classWrapper.delegatedSelfTypeRef
                else -> classWrapper.delegatedSuperTypeRef
            }

        return buildDelegatedConstructorCall {
            source = if (isImplicit) {
                constructorDelegationCall.toFirSourceElement().fakeElement(KtFakeSourceElementKind.ImplicitConstructor)
            } else {
                constructorDelegationCall.toFirSourceElement()
            }
            constructedTypeRef = delegatedType.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
            this.isThis = isThis
            val calleeKind =
                if (isImplicit) KtFakeSourceElementKind.ImplicitConstructor else KtFakeSourceElementKind.DelegatingConstructorCall
            val calleeSource = constructorDelegationCall.getChildNodeByTokenId(KtNodeTypeId.CONSTRUCTOR_DELEGATION_REFERENCE)
                ?.toFirSourceElement(calleeKind)
                ?: this@buildDelegatedConstructorCall.source?.fakeElement(calleeKind)
            calleeReference = if (isThis) {
                buildExplicitThisReference {
                    this.source = calleeSource
                }
            } else {
                buildExplicitSuperReference {
                    source = calleeSource
                    superTypeRef = this@buildDelegatedConstructorCall.constructedTypeRef
                }
            }
            extractArgumentsFrom(firValueArguments)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeAlias
     */
    fun convertTypeAlias(node: Node): FirTypeAlias {
        var modifiers: ModifierList<Node>? = null
        var identifier: String? = null
        lateinit var typeRefNode: Node
        var typeParametersNode: Node? = null

        node.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.MODIFIER_LIST -> modifiers = convertModifierList(it)
                KtTokenId.IDENTIFIER -> identifier = it.asText
                KtNodeTypeId.TYPE_REFERENCE -> typeRefNode = it
                KtNodeTypeId.TYPE_PARAMETER_LIST -> typeParametersNode = it
            }
        }

        val calculatedModifiers = modifiers ?: ModifierList()
        val typeAliasName = identifier.nameAsSafeName()
        val typeAliasIsExpect = calculatedModifiers.hasExpect() || context.containerIsExpect
        return context.withChildClassName(typeAliasName, isExpect = typeAliasIsExpect) {
            val typeAliasSymbol = FirTypeAliasSymbol(context.currentClassId)
            context.withContainerSymbol(typeAliasSymbol) {
                val isInner = calculatedModifiers.isInner()
                buildTypeAlias {
                    source = node.toFirSourceElement()
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    scopeProvider = baseScopeProvider
                    name = typeAliasName
                    val isLocal = context.inLocalContext
                    status = FirDeclarationStatusImpl(
                        if (isLocal) Visibilities.Local else calculatedModifiers.getVisibility(publicByDefault = true),
                        Modality.FINAL,
                    ).apply {
                        isExpect = typeAliasIsExpect
                        isActual = calculatedModifiers.hasActual()
                        this.isInner = isInner
                    }

                    symbol = typeAliasSymbol
                    expandedTypeRef = convertType(typeRefNode)
                    modifiers?.convertAnnotationsTo(annotations)
                    typeParametersNode?.let { typeParameters += convertTypeParameters(it, emptyList(), typeAliasSymbol) }

                    if (isInner || isLocal) {
                        context.appendOuterTypeParameters(ignoreLastLevel = false, typeParameters)
                    }
                }
            }
        }.also {
            if (node.getParent()?.toTokenId() == KtNodeTypeId.CLASS_BODY) {
                it.initContainingClassForLocalAttr()
            }
            if (isDirectlyInsideCompanionBlock) {
                it.isIllegalCompanionBlockMember = true
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseProperty
     */
    fun convertPropertyDeclaration(
        node: Node,
        classWrapper: ClassWrapper<Node>? = null,
        ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>? = classWrapper?.classBuilder?.ownerRegularOrAnonymousObjectSymbol,
    ): FirProperty {
        var modifiers: ModifierList<Node>? = null
        var identifier: String? = null
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        var isReturnType = false
        var delegate: Node? = null
        var isVar = false
        var receiverTypeNode: Node? = null
        var returnType: FirTypeRef = implicitType
        val typeConstraints = mutableListOf<TypeConstraint>()
        val accessors = mutableListOf<Node>()
        var propertyInitializer: FirExpression? = null
        var typeParameterList: Node? = null
        var fieldDeclaration: Node? = null
        node.getChildNodeByTokenId(KtTokenId.IDENTIFIER)?.let {
            identifier = it.asText
        }

        val isLocal = isCallableLocal(node) { getParent() }
        val isInsideScript = context.containingScriptSymbol != null && context.className == FqName.ROOT
        val propertyName = when {
            (isLocal || isInsideScript) && identifier == "_" -> SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
            else -> identifier.nameAsSafeName()
        }
        val propertySymbol = if (isLocal) {
            FirLocalPropertySymbol()
        } else {
            FirRegularPropertySymbol(callableIdForName(propertyName))
        }
        val isCompanionBlockMember = isDirectlyInsideCompanionBlock

        context.withContainerSymbol(propertySymbol, isLocal) {
            val propertySource = node.toFirSourceElement()
            node.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypeId.MODIFIER_LIST -> {
                        modifiers = convertModifierList(it)
                    }
                    KtNodeTypeId.TYPE_PARAMETER_LIST -> typeParameterList = it
                    KtTokenId.COLON -> isReturnType = true
                    KtNodeTypeId.TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverTypeNode = it
                    KtNodeTypeId.TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                    KtNodeTypeId.PROPERTY_DELEGATE -> delegate = it
                    KtTokenId.VAR_KEYWORD -> isVar = true
                    KtNodeTypeId.PROPERTY_ACCESSOR -> {
                        accessors += it
                    }
                    KtNodeTypeId.BACKING_FIELD -> fieldDeclaration = it
                    else -> if (it.isExpression()) {
                        context.calleeNamesForLambda += null
                        val keepBodyInHeaderMode =
                            !isReturnType || modifiers?.isConst() == true || keepPropertyBodiesInHeaderMode
                        propertyInitializer = runIf(isLocal || !headerMode || keepBodyInHeaderMode) {
                            context.withForcedLocalContext(keepBodyInHeaderMode) {
                                expressionConverter.getAsFirExpression(it, "Should have initializer")
                            }
                        }
                        context.calleeNamesForLambda.removeLast()
                    }
                }
            }

            val calculatedModifiers = modifiers ?: ModifierList()
            val propertyAnnotations = calculatedModifiers.convertAnnotations()
            val isStatic = calculatedModifiers.hasCompanion() || isCompanionBlockMember

            return buildProperty {
                source = propertySource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = returnType
                name = propertyName
                this.isVar = isVar

                receiverParameter = receiverTypeNode?.let {
                    createReceiverParameter(
                        { convertType(it) },
                        context,
                        moduleData,
                        propertySymbol
                    )
                }
                initializer = propertyInitializer

                //probably can do this for delegateExpression itself
                val delegateSource = delegate?.let {
                    (it.getFirstChildExpression() ?: it).toFirSourceElement()
                }

                symbol = propertySymbol
                this.isLocal = context.inLocalContext

                typeParameterList?.let { firTypeParameters += convertTypeParameters(it, typeConstraints, symbol) }

                backingField = fieldDeclaration.convertBackingField(
                    symbol, calculatedModifiers, returnType, isVar,
                    if (isLocal) emptyList() else propertyAnnotations.filter {
                        it.useSiteTarget == FIELD || it.useSiteTarget == PROPERTY_DELEGATE_FIELD
                    },
                    node,
                    isStatic,
                ).also {
                    if (!isLocal) {
                        it.initContainingClassAttr()
                    }
                }

                if (isLocal) {
                    val delegateBuilder = delegate?.let {
                        FirWrappedDelegateExpressionBuilder().apply {
                            source = delegateSource?.fakeElement(KtFakeSourceElementKind.WrappedDelegate)
                            expression = expressionConverter.getAsFirExpression(it, "Incorrect delegate expression")
                        }
                    }
                    status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL).apply {
                        isLateInit = calculatedModifiers.hasLateinit()
                    }

                    typeParameters += firTypeParameters
                    generateAccessorsByDelegate(
                        delegateBuilder,
                        baseModuleData,
                        ownerRegularOrAnonymousObjectSymbol,
                        context = context,
                        isExtension = false,
                        explicitDeclarationSource = propertySource,
                    )
                } else {
                    if (!isCompanionBlockMember) {
                        dispatchReceiverType = currentDispatchReceiverType(context)
                    }
                    context.withCapturedTypeParameters(true, propertySource, firTypeParameters) {
                        typeParameters += firTypeParameters

                        val delegateBuilder = delegate?.let {
                            FirWrappedDelegateExpressionBuilder().apply {
                                source = delegateSource?.fakeElement(KtFakeSourceElementKind.WrappedDelegate)
                                expression = expressionConverter.getAsFirExpression(it, "Should have delegate")
                            }
                        }

                        val propertyVisibility = calculatedModifiers.getVisibility()

                        fun defaultAccessorStatus() =
                            // Downward propagation of `inline` and `external` modifiers (from property to its accessors)
                            FirDeclarationStatusImpl(propertyVisibility, null).apply {
                                isInline = calculatedModifiers.hasInline()
                                isExternal = calculatedModifiers.hasExternal()
                                this.isStatic = isStatic
                            }

                        val convertedAccessors = accessors.map {
                            convertGetterOrSetter(it, returnType, propertyVisibility, symbol, calculatedModifiers, propertyAnnotations, isCompanionBlockMember)
                        }
                        this.getter = convertedAccessors.find { it.isGetter }
                            ?: FirDefaultPropertyGetter(
                                source = node.toFirSourceElement(KtFakeSourceElementKind.DefaultAccessor.Getter),
                                moduleData = moduleData,
                                origin = FirDeclarationOrigin.Source,
                                propertyTypeRef = returnType.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor.Getter),
                                visibility = propertyVisibility,
                                propertySymbol = symbol,
                                modality = calculatedModifiers.getModality(isClassOrObject = false),
                            ).also {
                                it.status = defaultAccessorStatus()
                                it.replaceAnnotations(propertyAnnotations.filterUseSiteTarget(PROPERTY_GETTER))
                                it.initContainingClassAttr()
                            }
                        // NOTE: We still need the setter even for a val property, so we can report errors (e.g., VAL_WITH_SETTER).
                        this.setter = convertedAccessors.find { it.isSetter }
                            ?: if (isVar) {
                                FirDefaultPropertySetter(
                                    source = node.toFirSourceElement(KtFakeSourceElementKind.DefaultAccessor.Setter),
                                    moduleData = moduleData,
                                    origin = FirDeclarationOrigin.Source,
                                    propertyTypeRef = returnType
                                        .copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor.Setter),
                                    visibility = propertyVisibility,
                                    propertySymbol = symbol,
                                    modality = calculatedModifiers.getModality(isClassOrObject = false),
                                    parameterAnnotations = propertyAnnotations.filterUseSiteTarget(SETTER_PARAMETER),
                                ).also {
                                    it.status = defaultAccessorStatus()
                                    it.replaceAnnotations(propertyAnnotations.filterUseSiteTarget(PROPERTY_SETTER))
                                    it.initContainingClassAttr()
                                }
                            } else null

                        status = FirDeclarationStatusImpl(
                            propertyVisibility, calculatedModifiers.getModality(isClassOrObject = false)
                        ).apply {
                            isExpect = calculatedModifiers.hasExpect() || context.containerIsExpect
                            isActual = calculatedModifiers.hasActual()
                            isOverride = calculatedModifiers.hasOverride()
                            isConst = calculatedModifiers.isConst()
                            isLateInit = calculatedModifiers.hasLateinit()
                            isExternal = calculatedModifiers.hasExternal()
                            this.isStatic = isStatic
                        }

                        generateAccessorsByDelegate(
                            delegateBuilder,
                            baseModuleData,
                            runUnless(isStatic) { ownerRegularOrAnonymousObjectSymbol },
                            context,
                            isExtension = receiverTypeNode != null && !isStatic,
                            explicitDeclarationSource = propertySource,
                        )
                    }
                }
                annotations += when {
                    isLocal -> propertyAnnotations
                    else -> propertyAnnotations.filterStandalonePropertyRelevantAnnotations(isVar)
                }

                contextParameters.addContextParameters(modifiers?.contextLists, propertySymbol)
            }.also {
                if (!isLocal) {
                    fillDanglingConstraintsTo(firTypeParameters, typeConstraints, it)

                    if (isCompanionBlockMember) {
                        it.initContainingClassAttr()
                    }
                }
            }
        }
    }

    /**
     * see PsiRawFirBuilder.Visitor.visitDestructuringDeclaration
     */
    fun convertDestructingDeclaration(node: Node, convertAnnotations: Boolean = true): DestructuringDeclaration {
        val annotations = mutableListOf<FirAnnotationCall>()
        var isVar = false
        var hasSquareBrackets = false
        val entries = mutableListOf<DestructuringEntry>()
        val source = node.toFirSourceElement()
        var firExpression: FirExpression? = null
        node.forEachChildren {
            when (it.toTokenId()) {
                KtTokenId.LBRACKET -> hasSquareBrackets = true
                KtNodeTypeId.MODIFIER_LIST -> if (convertAnnotations) convertAnnotationsOnlyTo(it, annotations)
                KtTokenId.VAR_KEYWORD -> isVar = true
                KtNodeTypeId.DESTRUCTURING_DECLARATION_ENTRY -> entries += convertDestructingDeclarationEntry(it, isVar)
                // Property delegates should be ignored as they aren't a valid initializer
                KtNodeTypeId.PROPERTY_DELEGATE -> {}
                else -> if (it.isExpression()) firExpression =
                    expressionConverter.getAsFirExpression(it, "Initializer required for destructuring declaration")
            }
        }

        // The parser wraps the brackets and the entries of a non-local destructuring declaration in an error element (KT-58563),
        // which `forEachChildren` skips
        if (entries.isEmpty()) {
            for (child in node.getChildren()) {
                if (child.toTokenId() != BasicElementTypeId.ERROR_ELEMENT) continue
                for (errorChild in child.getChildren()) {
                    when (errorChild.toTokenId()) {
                        KtTokenId.LBRACKET -> hasSquareBrackets = true
                        KtNodeTypeId.DESTRUCTURING_DECLARATION_ENTRY -> entries += convertDestructingDeclarationEntry(errorChild, isVar)
                    }
                }
            }
        }

        return DestructuringDeclaration(
            isVar = isVar,
            kind = destructuringKindOf(hasSquareBrackets = hasSquareBrackets, isFullForm = entries.any { it.isFullForm }),
            entries,
            firExpression ?: buildErrorExpression(
                node.toFirSourceElement(),
                ConeSyntaxDiagnostic("Initializer required for destructuring declaration")
            ),
            source,
            annotations
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseMultiDeclarationEntry
     */
    private fun convertDestructingDeclarationEntry(entry: Node, isVar: Boolean): DestructuringEntry {
        var modifierList: Node? = null
        var identifier: String? = null
        var typeReference: Node? = null
        var isVar = isVar
        var initializerName: Name? = null
        var initializerSource: KtSourceElement? = null
        var isFullForm = false
        entry.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.MODIFIER_LIST -> modifierList = it
                KtTokenId.IDENTIFIER -> identifier = it.asText
                KtNodeTypeId.TYPE_REFERENCE -> typeReference = it
                KtTokenId.VAL_KEYWORD -> isFullForm = true
                KtTokenId.VAR_KEYWORD -> {
                    isFullForm = true
                    isVar = true
                }
                KtNodeTypeId.REFERENCE_EXPRESSION -> {
                    initializerName = it.getReferencedNameAsName()
                    initializerSource = it.toFirSourceElement()
                }
            }
        }

        val name = if (identifier == "_") {
            SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
        } else {
            identifier.nameAsSafeName()
        }

        // The type and the annotations are converted on demand, under the symbol of the entry, as in PsiRawFirBuilder
        return DestructuringEntry(
            source = entry.toFirSourceElement(),
            initializerSource = initializerSource,
            convertReturnTypeRef = { typeReference?.let(::convertType) ?: implicitType },
            name = name,
            initializerName = initializerName,
            isVar = isVar,
            isFullForm = isFullForm,
            convertAnnotationsTo = { annotations -> modifierList?.let { convertAnnotationsOnlyTo(it, annotations) } },
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyComponent
     */
    private fun convertGetterOrSetter(
        getterOrSetter: Node,
        propertyTypeRef: FirTypeRef,
        propertyVisibility: Visibility,
        propertySymbol: FirPropertySymbol,
        propertyModifiers: ModifierList<Node>,
        propertyAnnotations: List<FirAnnotationCall>,
        isCompanionBlockMember: Boolean,
    ): FirPropertyAccessor {
        var modifiers: ModifierList<Node>? = null
        var isGetter = true
        var returnType: FirTypeRef? = null
        val propertyTypeRefToUse = propertyTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
        val sourceElement = getterOrSetter.toFirSourceElement()
        val accessorSymbol = FirPropertyAccessorSymbol()
        var firValueParameters: FirValueParameter = buildDefaultSetterValueParameter {
            moduleData = baseModuleData
            containingDeclarationSymbol = accessorSymbol
            origin = FirDeclarationOrigin.Source
            source = sourceElement.fakeElement(KtFakeSourceElementKind.DefaultAccessor.Setter.ValueParameter)
            returnTypeRef = propertyTypeRefToUse
            symbol = FirValueParameterSymbol()
        }
        var block: Node? = null
        var expression: Node? = null
        var outerContractDescription: FirContractDescription? = null


        getterOrSetter.forEachChildren {
            if (it.asText == "set") isGetter = false
            when (it.toTokenId()) {
                KtTokenId.SET_KEYWORD -> isGetter = false
                KtNodeTypeId.MODIFIER_LIST -> {
                    modifiers = convertModifierList(it)
                }
                KtNodeTypeId.TYPE_REFERENCE -> returnType = convertType(it)
                KtNodeTypeId.VALUE_PARAMETER_LIST -> {
                    // getter can have an empty value parameter list
                    if (!isGetter) {
                        firValueParameters = convertSetterParameter(
                            it, accessorSymbol, propertyTypeRefToUse, propertyAnnotations.filterUseSiteTarget(SETTER_PARAMETER)
                        )
                    }
                }
                KtNodeTypeId.CONTRACT_EFFECT_LIST -> outerContractDescription = obtainContractDescription(it)
                KtNodeTypeId.BLOCK -> block = it
                else -> if (it.isExpression()) expression = it
            }
        }

        val calculatedModifiers = modifiers ?: ModifierList()
        var accessorVisibility = calculatedModifiers.getVisibility()
        if (accessorVisibility == Visibilities.Unknown) {
            accessorVisibility = propertyVisibility
        }
        val status =
            // Downward propagation of `inline`, `external` and `expect` modifiers (from property to its accessors)
            FirDeclarationStatusImpl(accessorVisibility, calculatedModifiers.getModality(isClassOrObject = false)).apply {
                isInline = propertyModifiers.hasInline() || calculatedModifiers.hasInline()
                isExternal = propertyModifiers.hasExternal() || calculatedModifiers.hasExternal()
                isExpect = propertyModifiers.hasExpect() || calculatedModifiers.hasExpect()
                isStatic = propertyModifiers.hasCompanion() || isCompanionBlockMember
            }
        val accessorAdditionalAnnotations = propertyAnnotations.filterUseSiteTarget(
            if (isGetter) PROPERTY_GETTER
            else PROPERTY_SETTER
        )
        val accessorAnnotations = calculatedModifiers.convertAnnotations()
        if (block == null && expression == null) {
            return FirDefaultPropertyAccessor
                .createGetterOrSetter(
                    sourceElement,
                    baseModuleData,
                    FirDeclarationOrigin.Source,
                    propertyTypeRefToUse,
                    accessorVisibility,
                    propertySymbol,
                    isGetter,
                    parameterSource = firValueParameters.source,
                )
                .also { accessor ->
                    accessor.replaceAnnotations(accessorAnnotations + accessorAdditionalAnnotations)
                    accessor.status = status
                    accessor.initContainingClassAttr()
                    accessor.valueParameters.firstOrNull()?.replaceReturnTypeRef(firValueParameters.returnTypeRef)
                }
        }
        val target = FirFunctionTarget(labelName = null, isLambda = false)
        return buildPropertyAccessor {
            source = sourceElement
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = returnType ?: if (isGetter) propertyTypeRefToUse else implicitUnitType
            symbol = accessorSymbol
            this.isGetter = isGetter
            this.status = status
            context.firFunctionTargets += target
            annotations += accessorAdditionalAnnotations
            annotations += accessorAnnotations

            if (!isGetter) {
                valueParameters += firValueParameters
            }
            val allowLegacyContractDescription = outerContractDescription == null
            val bodyWithContractDescription = context.withForcedLocalContext(
                forceKeepingTheBodyInHeaderMode = propertyTypeRef is FirImplicitTypeRef || status.isInline || keepPropertyBodiesInHeaderMode
            ) {
                convertFunctionBody(block, expression, allowLegacyContractDescription)
            }
            this.body = bodyWithContractDescription.first
            val contractDescription = outerContractDescription ?: bodyWithContractDescription.second
            contractDescription?.let {
                this.contractDescription = it
            }
            context.firFunctionTargets.removeLast()
            this.propertySymbol = propertySymbol
        }.also {
            target.bind(it)
            it.initContainingClassAttr()
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyComponent
     */
    private fun Node?.convertBackingField(
        propertySymbol: FirPropertySymbol,
        propertyModifiers: ModifierList<Node>,
        propertyReturnType: FirTypeRef,
        isVar: Boolean,
        annotationsFromProperty: List<FirAnnotationCall>,
        property: Node,
        isStatic: Boolean,
    ): FirBackingField {
        var modifiers: ModifierList<Node>? = null
        var returnType: FirTypeRef = implicitType
        var backingFieldInitializer: FirExpression? = null
        this?.forEachChildren {
            when {
                it.toTokenId() == KtNodeTypeId.MODIFIER_LIST -> {
                    modifiers = convertModifierList(it)
                }
                it.toTokenId() == KtNodeTypeId.TYPE_REFERENCE -> returnType = convertType(it)
                it.isExpression() -> {
                    backingFieldInitializer = expressionConverter.getAsFirExpression(it, "Should have initializer")
                }
            }
        }
        val calculatedModifiers = modifiers ?: ModifierList()
        val status = obtainPropertyComponentStatus(Visibilities.Private, calculatedModifiers, propertyModifiers, isStatic)
        val sourceElement = this?.toFirSourceElement()
        return if (this != null) {
            buildBackingField {
                source = sourceElement
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = returnType
                name = StandardNames.BACKING_FIELD
                symbol = FirBackingFieldSymbol()
                this.status = status
                modifiers?.convertAnnotationsTo(annotations)
                annotations += annotationsFromProperty
                this.propertySymbol = propertySymbol
                this.initializer = backingFieldInitializer
                this.isVar = isVar
                this.isVal = !isVar
            }
        } else {
            FirDefaultPropertyBackingField(
                moduleData = baseModuleData,
                origin = FirDeclarationOrigin.Source,
                source = property.toFirSourceElement(KtFakeSourceElementKind.DefaultAccessor.BackingField),
                annotations = annotationsFromProperty.toMutableList(),
                returnTypeRef = propertyReturnType.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor.BackingField),
                isVar = isVar,
                propertySymbol = propertySymbol,
                status = status,
            )
        }
    }

    private fun obtainPropertyComponentStatus(
        componentVisibility: Visibility,
        modifiers: ModifierList<Node>,
        propertyModifiers: ModifierList<Node>,
        isStatic: Boolean,
    ): FirDeclarationStatus {
        // Downward propagation of `inline` and `external` modifiers (from property to its accessors)
        return FirDeclarationStatusImpl(componentVisibility, modifiers.getModality(isClassOrObject = false)).apply {
            isInline = propertyModifiers.hasInline() || modifiers.hasInline()
            isExternal = propertyModifiers.hasExternal() || modifiers.hasExternal()
            isLateInit = modifiers.hasLateinit()
            this.isStatic = isStatic
        }
    }

    private fun obtainContractDescription(rawContractDescription: Node): FirContractDescription =
        buildRawContractDescription {
            source = rawContractDescription.toFirSourceElement()
            extractRawEffects(rawContractDescription, rawEffects)
        }

    private fun extractRawEffects(rawContractDescription: Node, destination: MutableList<FirExpression>) {
        rawContractDescription.forEachChildren {
            val errorReason = "The contract effect is not an expression"
            when (it.toTokenId()) {
                KtNodeTypeId.CONTRACT_EFFECT -> {
                    val effect = it.getFirstChild()
                    val expression = if (effect == null) {
                        buildErrorExpression(
                            rawContractDescription.toFirSourceElement(),
                            ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected)
                        )
                    } else {
                        expressionConverter.getAsFirExpression<FirExpression>(effect, errorReason)
                    }
                    destination.add(expression)
                }
                else -> Unit
            }
        }
    }

    /**
     * this is just a VALUE_PARAMETER_LIST
     *
     * see PsiRawFirBuilder.Visitor.toFirValueParameter
     *
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyComponent
     */
    private fun convertSetterParameter(
        setterParameter: Node,
        functionSymbol: FirFunctionSymbol<*>,
        propertyTypeRef: FirTypeRef,
        additionalAnnotations: List<FirAnnotation>,
    ): FirValueParameter {
        var modifiers: ModifierList<Node>? = null
        lateinit var firValueParameter: FirValueParameter
        setterParameter.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.MODIFIER_LIST -> modifiers = convertModifierList(it)
                KtNodeTypeId.VALUE_PARAMETER -> firValueParameter =
                    convertValueParameter(it, functionSymbol, ValueParameterDeclaration.SETTER).firValueParameter
            }
        }

        val calculatedModifiers = modifiers ?: ModifierList()
        return buildValueParameter {
            source = firValueParameter.source
            containingDeclarationSymbol = functionSymbol
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = if (firValueParameter.returnTypeRef == implicitType) propertyTypeRef else firValueParameter.returnTypeRef
            name = firValueParameter.name
            symbol = FirValueParameterSymbol()
            defaultValue = firValueParameter.defaultValue
            isCrossinline = calculatedModifiers.hasCrossinline() || firValueParameter.isCrossinline
            isNoinline = calculatedModifiers.hasNoinline() || firValueParameter.isNoinline
            isVararg = calculatedModifiers.hasVararg() || firValueParameter.isVararg
            annotations += firValueParameter.annotations
            annotations += additionalAnnotations
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunction
     */
    fun convertFunctionDeclaration(node: Node): FirStatement {
        var modifiers: ModifierList<Node>? = null
        var identifier: String? = null
        var valueParametersList: Node? = null
        var isReturnType = false
        var receiverTypeNode: Node? = null
        var returnType: FirTypeRef? = null
        val typeConstraints = mutableListOf<TypeConstraint>()
        var block: Node? = null
        var expression: Node? = null
        var hasEqToken = false
        var typeParameterList: Node? = null
        var outerContractDescription: FirContractDescription? = null
        node.getChildNodeByTokenId(KtTokenId.IDENTIFIER)?.let {
            identifier = it.asText
        }

        val isLocal = isCallableLocal(node) { getParent() }
        val functionSource = node.toFirSourceElement()
        val isAnonymousFunction = identifier == null && isLocal
        val functionName = identifier.nameAsSafeName()
        val functionSymbol: FirFunctionSymbol<*> = if (isAnonymousFunction) {
            FirAnonymousFunctionSymbol()
        } else {
            FirNamedFunctionSymbol(callableIdForName(functionName))
        }
        val isCompanionBlockMember = isDirectlyInsideCompanionBlock

        context.withContainerSymbol(functionSymbol, isLocal) {
            val target: FirFunctionTarget
            node.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypeId.MODIFIER_LIST -> {
                        modifiers = convertModifierList(it)
                    }
                    KtNodeTypeId.TYPE_PARAMETER_LIST -> typeParameterList = it
                    KtNodeTypeId.VALUE_PARAMETER_LIST -> valueParametersList = it //must convert later, because it can contain "return"
                    KtTokenId.COLON -> isReturnType = true
                    KtNodeTypeId.TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverTypeNode = it
                    KtNodeTypeId.TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                    KtNodeTypeId.CONTRACT_EFFECT_LIST -> outerContractDescription = obtainContractDescription(it)
                    KtNodeTypeId.BLOCK -> block = it
                    KtTokenId.EQ -> hasEqToken = true
                    else -> if (it.isExpression()) expression = it
                }
            }

            val calculatedModifiers = modifiers ?: ModifierList()

            if (returnType == null) {
                returnType =
                    if (block != null || !hasEqToken) implicitUnitType
                    else implicitType
            }

            val receiverTypeCalculator = receiverTypeNode?.let { { convertType(it) } }
            val functionBuilder = if (isAnonymousFunction) {
                FirAnonymousFunctionBuilder().apply {
                    source = functionSource
                    receiverParameter = receiverTypeCalculator?.let { createReceiverParameter(it, context, baseModuleData, functionSymbol) }
                    symbol = functionSymbol as FirAnonymousFunctionSymbol
                    isLambda = false
                    hasExplicitParameterList = true
                    label = context.getLastLabel(node)
                    val labelName = label?.name ?: context.calleeNamesForLambda.lastOrNull()?.identifier
                    target = FirFunctionTarget(labelName = labelName, isLambda = false)

                    val isExpect = calculatedModifiers.hasExpect() || context.containerIsExpect
                    val isActual = calculatedModifiers.hasActual()
                    val isOverride = calculatedModifiers.hasOverride()
                    val isOperator = calculatedModifiers.hasOperator()
                    val isInfix = calculatedModifiers.hasInfix()
                    val isInline = calculatedModifiers.hasInline()
                    val isTailRec = calculatedModifiers.hasTailrec()
                    val isExternal = calculatedModifiers.hasExternal()
                    val isSuspend = calculatedModifiers.hasSuspend()

                    if (isExpect || isActual || isOverride || isOperator || isInfix || isInline || isTailRec || isExternal || isSuspend) {
                        status = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.copy(
                            isExpect = isExpect,
                            isActual = isActual,
                            isOverride = isOverride,
                            isOperator = isOperator,
                            isInfix = isInfix,
                            isInline = isInline,
                            isTailRec = isTailRec,
                            isExternal = isExternal,
                            isSuspend = isSuspend,
                        )
                    }
                }
            } else {
                val labelName =
                    context.getLastLabel(node)?.name ?: runIf(!functionName.isSpecial) { functionName.identifier }
                target = FirFunctionTarget(labelName, isLambda = false)
                FirNamedFunctionBuilder().apply {
                    source = functionSource
                    receiverParameter = receiverTypeCalculator?.let { createReceiverParameter(it, context, baseModuleData, functionSymbol) }
                    name = functionName
                    this.isLocal = context.inLocalContext
                    status = FirDeclarationStatusImpl(
                        if (isLocal) Visibilities.Local else calculatedModifiers.getVisibility(),
                        calculatedModifiers.getModality(isClassOrObject = false)
                    ).apply {
                        isExpect = calculatedModifiers.hasExpect() || context.containerIsExpect
                        isActual = calculatedModifiers.hasActual()
                        isOverride = calculatedModifiers.hasOverride()
                        isOperator = calculatedModifiers.hasOperator()
                        isInfix = calculatedModifiers.hasInfix()
                        isInline = calculatedModifiers.hasInline()
                        isTailRec = calculatedModifiers.hasTailrec()
                        isExternal = calculatedModifiers.hasExternal()
                        isSuspend = calculatedModifiers.hasSuspend()
                        isStatic = calculatedModifiers.hasCompanion() || isCompanionBlockMember
                    }

                    symbol = functionSymbol as FirNamedFunctionSymbol
                    dispatchReceiverType = runIf(!isLocal && !isCompanionBlockMember) { currentDispatchReceiverType(context) }
                }
            }

            val firTypeParameters = mutableListOf<FirTypeParameter>()
            typeParameterList?.let { firTypeParameters += convertTypeParameters(it, typeConstraints, functionSymbol) }

            val function = functionBuilder.apply {
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = returnType

                context.firFunctionTargets += target
                modifiers?.convertAnnotationsTo(annotations)
                typeParameters += firTypeParameters

                context.withCapturedTypeParameters(true, functionSource, typeParameters) {
                    contextParameters.addContextParameters(modifiers?.contextLists, functionSymbol)

                    valueParametersList?.let { list ->
                        valueParameters += convertValueParameters(
                            list,
                            functionSymbol,
                            if (isAnonymousFunction) ValueParameterDeclaration.LAMBDA else ValueParameterDeclaration.FUNCTION
                        ).map { it.firValueParameter }
                    }

                    val allowLegacyContractDescription = outerContractDescription == null
                    val bodyWithContractDescription = context.withForcedLocalContext(
                        forceKeepingTheBodyInHeaderMode = functionBuilder.status.isInline || functionBuilder.returnTypeRef is FirImplicitTypeRef
                    ) {
                        convertFunctionBody(block, expression, allowLegacyContractDescription)
                    }
                    this.body = bodyWithContractDescription.first
                    val contractDescription = outerContractDescription ?: bodyWithContractDescription.second
                    contractDescription?.let {
                        if (this is FirNamedFunctionBuilder) {
                            this.contractDescription = it
                        } else if (this is FirAnonymousFunctionBuilder) {
                            this.contractDescription = it
                        }
                    }
                }
                context.firFunctionTargets.removeLast()
            }.build().also {
                target.bind(it)
                fillDanglingConstraintsTo(firTypeParameters, typeConstraints, it)

                if (!isLocal && isCompanionBlockMember) {
                    it.initContainingClassAttr()
                }
            }

            return if (function is FirAnonymousFunction) {
                buildAnonymousFunctionExpression {
                    source = functionSource
                    anonymousFunction = function
                }
            } else {
                function
            }
        }
    }

    private fun isCallableLocal(callableNode: Node, getParent: Node.() -> Node?): Boolean {
        val parentNode = callableNode.getParent()
        return when (parentNode?.toTokenId()) {
            KtNodeTypeId.FILE, KtNodeTypeId.CLASS_BODY -> false
            KtNodeTypeId.BLOCK -> when (parentNode.getParent()?.toTokenId()) {
                KtNodeTypeId.SCRIPT -> false
                else -> true
            }
            else -> true
        }
    }

    /**
     * see PsiRawFirBuilder.Visitor.buildFirBody
     *
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionBody
     */
    private fun convertFunctionBody(
        blockNode: Node?,
        expression: Node?,
        allowLegacyContractDescription: Boolean
    ): Pair<FirBlock?, FirContractDescription?> {
        val generateHeader = headerMode && !context.forceKeepingTheBodyInHeaderMode
        return when {
            blockNode != null -> {
                val block = convertBlock(blockNode, convertOnlyFirstStatement = generateHeader)
                val contractDescription = runIf(allowLegacyContractDescription) {
                    val blockSource = block.source
                    val diagnostic = when {
                        blockSource == null || !isCallTheFirstStatement(blockSource) -> ConeContractShouldBeFirstStatement
                        functionCallHasLabel(blockSource) -> ConeContractMayNotHaveLabel
                        else -> null
                    }
                    processLegacyContractDescription(block, diagnostic)
                }
                if (generateHeader) {
                    buildEmptyExpressionBlock() to contractDescription
                } else {
                    block to contractDescription
                }
            }
            expression != null -> {
                if (generateHeader) buildEmptyExpressionBlock() to null
                else FirSingleExpressionBlock(
                    expressionConverter.getAsFirExpression<FirExpression>(expression, "Function has no body (but should)").toReturn()
                ) to null
            }
            else -> null to null
        }
    }

    private fun isCallTheFirstStatement(sourceElement: KtSourceElement): Boolean =
        isCallTheFirstStatement(
            sourceElement.toNode(),
            { it.toTokenId() },
            { it.getChildren() })

    private fun functionCallHasLabel(sourceElement: KtSourceElement): Boolean {
        return firstFunctionCallInBlockHasLambdaArgumentWithLabel(
            sourceElement.toNode(),
            { it.toTokenId() },
            { it.getChildren() })
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseBlock
     */
    fun convertBlock(
        node: Node?,
        convertOnlyFirstStatement: Boolean = false,
    ): FirBlock {
        if (node == null) return buildEmptyExpressionBlock()
        if (node.toTokenId() != KtNodeTypeId.BLOCK) {
            return FirSingleExpressionBlock(
                expressionConverter.getAsFirStatement(node)
            )
        }

        return convertBlockExpression(node, convertOnlyFirstStatement)
    }

    /**
     * see PsiRawFirBuilder.Visitor.extractSuperTypeListEntriesTo
     *
     * SUPER_TYPE_ENTRY             - userType
     * SUPER_TYPE_CALL_ENTRY        - constructorInvocation
     * DELEGATED_SUPER_TYPE_ENTRY   - explicitDelegation
     *
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseDelegationSpecifierList
     */
    //TODO make wrapper for result?
    private data class DelegationSpecifiers(
        val superTypeCalls: List<DelegatedConstructorWrapper>,
        val superTypesRef: List<FirTypeRef>,
        val delegateFieldsMap: Map<Int, FirFieldSymbol>,
    )

    private fun convertDelegationSpecifiers(delegationSpecifiers: Node): DelegationSpecifiers {
        val superTypeRefs = mutableListOf<FirTypeRef>()
        val superTypeCalls = mutableListOf<DelegatedConstructorWrapper>()
        val delegateFieldsMap = mutableMapOf<Int, FirFieldSymbol>()
        var index = 0
        delegationSpecifiers.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.SUPER_TYPE_ENTRY -> {
                    superTypeRefs += convertType(it)
                    index++
                }
                KtNodeTypeId.SUPER_TYPE_CALL_ENTRY -> convertConstructorInvocation(it).apply {
                    superTypeCalls += DelegatedConstructorWrapper(first, second, it.toFirSourceElement() as KtLightSourceElement)
                    superTypeRefs += first
                    index++
                }
                KtNodeTypeId.DELEGATED_SUPER_TYPE_ENTRY -> {
                    superTypeRefs += convertExplicitDelegation(it, delegateFieldsMap, index)
                    index++
                }
            }
        }
        return DelegationSpecifiers(superTypeCalls, superTypeRefs, delegateFieldsMap)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseDelegationSpecifier
     *
     * constructorInvocation
     *   : userType valueArguments
     *   ;
     */
    private fun convertConstructorInvocation(constructorInvocation: Node): Pair<FirTypeRef, List<FirExpression>> {
        var firTypeRef: FirTypeRef = implicitType
        val firValueArguments = mutableListOf<FirExpression>()
        constructorInvocation.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.CONSTRUCTOR_CALLEE -> firTypeRef = convertType(it)
                KtNodeTypeId.VALUE_ARGUMENT_LIST -> firValueArguments += expressionConverter.convertValueArguments(it)
            }
        }
        return Pair(firTypeRef, firValueArguments)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseDelegationSpecifier
     *
     * explicitDelegation
     *   : userType "by" element
     *   ;
     */
    private fun convertExplicitDelegation(
        explicitDelegation: Node,
        delegateFieldsMap: MutableMap<Int, FirFieldSymbol>,
        index: Int
    ): FirTypeRef {
        lateinit var firTypeRef: FirTypeRef
        var expressionNode: Node? = null
        explicitDelegation.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.TYPE_REFERENCE -> firTypeRef = convertType(it)
                else -> if (it.isExpression()) expressionNode = it
            }
        }

        delegateFieldsMap[index] = buildField {
            source = explicitDelegation.toFirSourceElement().fakeElement(KtFakeSourceElementKind.ClassDelegationField)
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Synthetic.DelegateField
            name = NameUtils.delegateFieldName(delegateFieldsMap.size)
            symbol = FirFieldSymbol(CallableId(context.currentClassId, name))
            returnTypeRef = firTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ClassDelegationField)
            context.withContainerSymbol(symbol) {
                val errorReason = "Should have delegate"
                initializer = expressionNode?.let {
                    expressionConverter.getAsFirExpression(it, errorReason)
                } ?: buildErrorExpression(explicitDelegation.toFirSourceElement(), ConeSyntaxDiagnostic(errorReason))
            }

            isVar = false
            status = FirDeclarationStatusImpl(Visibilities.Private, Modality.FINAL)
            isLocal = context.inLocalContext
            dispatchReceiverType = currentDispatchReceiverType(context)
        }.symbol
        return firTypeRef
    }

    /*****    TYPES    *****/
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeParameterList
     */
    private fun convertTypeParameters(
        typeParameterList: Node,
        typeConstraints: List<TypeConstraint>,
        containingDeclarationSymbol: FirBasedSymbol<*>
    ): List<FirTypeParameter> {
        return typeParameterList.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypeId.TYPE_PARAMETER -> container += convertTypeParameter(node, typeConstraints, containingDeclarationSymbol)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeConstraintList
     */
    private fun convertTypeConstraints(typeConstraints: Node): List<TypeConstraint> {
        return typeConstraints.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypeId.TYPE_CONSTRAINT -> container += convertTypeConstraint(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeConstraint
     */
    private fun convertTypeConstraint(typeConstraint: Node): TypeConstraint {
        var identifier: String? = null
        var firType: FirTypeRef? = null
        var referenceExpression: Node? = null

        val annotations = mutableListOf<FirAnnotation>()
        typeConstraint.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.ANNOTATION_ENTRY -> {
                    annotations +=
                        convertAnnotationEntry(
                            it,
                            diagnostic = ConeSimpleDiagnostic(
                                "Type parameter annotations are not allowed inside where clauses", DiagnosticKind.AnnotationInWhereClause,
                            )
                        )
                }
                KtNodeTypeId.REFERENCE_EXPRESSION -> {
                    identifier = it.asText
                    referenceExpression = it
                }
                KtNodeTypeId.TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        return TypeConstraint(
            annotations,
            identifier,
            firType ?: buildErrorTypeRef { },
            (referenceExpression ?: typeConstraint).toFirSourceElement()
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeParameter
     */
    private fun convertTypeParameter(
        typeParameter: Node,
        typeConstraints: List<TypeConstraint>,
        containingSymbol: FirBasedSymbol<*>
    ): FirTypeParameter {
        var typeParameterModifiers: TypeParameterModifierList<Node>? = null
        var identifier: String? = null
        var firType: FirTypeRef? = null
        typeParameter.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.MODIFIER_LIST -> typeParameterModifiers = convertTypeParameterModifiers(it)
                KtTokenId.IDENTIFIER -> identifier = it.asText
                KtNodeTypeId.TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val calculatedTypeParameterModifiers = typeParameterModifiers ?: TypeParameterModifierList()
        return buildTypeParameter {
            source = typeParameter.toFirSourceElement()
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            name = identifier.nameAsSafeName()
            symbol = FirTypeParameterSymbol()
            containingDeclarationSymbol = containingSymbol
            variance = calculatedTypeParameterModifiers.getVariance()
            isReified = calculatedTypeParameterModifiers.hasReified()
            typeParameterModifiers?.convertAnnotationsTo(annotations)
            firType?.let { bounds += it }
            for (typeConstraint in typeConstraints) {
                if (typeConstraint.identifier == identifier) {
                    bounds += typeConstraint.firTypeRef
                    annotations += typeConstraint.annotations
                }
            }
            addDefaultBoundIfNecessary()
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeRef
     */
    fun convertType(type: Node): FirTypeRef {
        val typeRefSource = type.toFirSourceElement()

        // There can be MODIFIER_LIST children on the TYPE_REFERENCE node AND the descendant NULLABLE_TYPE nodes.
        // We aggregate them to get modifiers and annotations. Not only that, there could be multiple modifier lists on each. Examples:
        //
        //   `@A() (@B Int)`   -> Has 2 modifier lists (@A and @B) in TYPE_REFERENCE
        //   `(@A() (@B Int))? -> No modifier list on TYPE_REFERENCE, but 2 modifier lists (@A and @B) on child NULLABLE_TYPE
        //   `@A() (@B Int)?   -> Has 1 modifier list (@A) on TYPE_REFERENCE, and 1 modifier list (@B) on child NULLABLE_TYPE
        //   `@A (@B() (@C() (@Bar D)?)?)?` -> Has 1 modifier list (@A) on B and 1 modifier list on each of the
        //                                     3 descendant NULLABLE_TYPE (@B, @C, @D)
        //
        // We need to examine all modifier lists for some cases:
        // 1. `@A Int?` and `(@A Int)?` are effectively the same, but in the latter, the modifier list is on the child NULLABLE_TYPE
        // 2. `(suspend @A () -> Int)?` is a nullable suspend function type, but the modifier list is on the child NULLABLE_TYPE
        //
        val allTypeModifiers = mutableListOf<ModifierList<Node>>()

        var firType: FirTypeRef? = null
        type.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.TYPE_REFERENCE -> firType = convertType(it)
                KtNodeTypeId.MODIFIER_LIST -> allTypeModifiers += convertModifierList(it)
                KtNodeTypeId.USER_TYPE -> firType = convertUserType(typeRefSource, it)
                KtNodeTypeId.NULLABLE_TYPE -> firType = convertNullableType(typeRefSource, it, allTypeModifiers)
                KtNodeTypeId.FUNCTION_TYPE -> firType = convertFunctionType(typeRefSource, it, allTypeModifiers)
                KtNodeTypeId.DYNAMIC_TYPE -> firType = buildDynamicTypeRef {
                    source = typeRefSource
                    isMarkedNullable = false
                }
                KtNodeTypeId.INTERSECTION_TYPE -> firType = convertIntersectionType(typeRefSource, it, false)
                KtNodeTypeId.UNION_TYPE -> firType = convertUnionType(typeRefSource, it, false)
                KtNodeTypeId.CONTEXT_PARAMETER_LIST, NO_ID -> firType =
                    buildErrorTypeRef {
                        source = typeRefSource
                        diagnostic = ConeSyntaxDiagnostic("Unwrapped type is null")
                    }
            }
        }

        val calculatedFirType = firType ?: buildErrorTypeRef {
            source = typeRefSource
            diagnostic = ConeSyntaxDiagnostic("Incomplete code")
        }

        for (modifierList in allTypeModifiers) {
            calculatedFirType.replaceAnnotations(calculatedFirType.annotations.smartPlus(modifierList.convertAnnotations()))
        }
        return calculatedFirType
    }

    private fun convertIntersectionType(typeRefSource: KtSourceElement, intersectionType: Node, isNullable: Boolean): FirTypeRef {
        val children = arrayListOf<FirTypeRef>()
        intersectionType.forEachChildren {
            if (it.toTokenId() != KtTokenId.AND) { //skip in forEachChildren?
                children.add(convertType(it))
            }
        }

        if (children.size != 2) {
            return buildErrorTypeRef {
                source = typeRefSource
                diagnostic = ConeSyntaxDiagnostic("Wrong code")
            }
        }

        return buildIntersectionTypeRef {
            source = typeRefSource
            isMarkedNullable = isNullable
            leftType = children[0]
            rightType = children[1]
        }
    }

    private fun convertUnionType(typeRefSource: KtSourceElement, unionType: Node, isNullable: Boolean): FirTypeRef {
        val children = arrayListOf<FirTypeRef>()
        unionType.forEachChildren {
            if (it.toTokenId() != KtTokenId.OR) {
                children.add(convertType(it))
            }
        }

        return buildUnionTypeRef {
            source = typeRefSource
            isMarkedNullable = isNullable
            types.addAll(children)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeRefContents
     */
    private fun convertReceiverType(receiverType: Node): FirTypeRef {
        receiverType.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.TYPE_REFERENCE -> return convertType(it)
            }
        }

        //TODO specify error
        throw Exception()
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseNullableTypeSuffix
     */
    private fun convertNullableType(
        typeRefSource: KtSourceElement,
        nullableType: Node,
        allTypeModifiers: MutableList<ModifierList<Node>>,
        isNullable: Boolean = true
    ): FirTypeRef {
        lateinit var firType: FirTypeRef
        nullableType.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.MODIFIER_LIST -> allTypeModifiers += convertModifierList(it)
                KtNodeTypeId.USER_TYPE -> firType = convertUserType(typeRefSource, it, isNullable)
                KtNodeTypeId.FUNCTION_TYPE -> firType = convertFunctionType(typeRefSource, it, allTypeModifiers, isNullable)
                KtNodeTypeId.NULLABLE_TYPE -> firType = convertNullableType(typeRefSource, it, allTypeModifiers)
                KtNodeTypeId.DYNAMIC_TYPE -> firType = buildDynamicTypeRef {
                    source = typeRefSource
                    isMarkedNullable = true
                }
                KtNodeTypeId.INTERSECTION_TYPE -> firType = convertIntersectionType(typeRefSource, it, isNullable)
                KtNodeTypeId.UNION_TYPE -> firType = convertUnionType(typeRefSource, it, isNullable)
            }
        }

        return firType
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseUserType
     */
    private fun convertUserType(
        typeRefSource: KtSourceElement,
        userType: Node,
        isNullable: Boolean = false
    ): FirTypeRef {
        var simpleFirUserType: FirUserTypeRef? = null
        var identifier: String? = null
        var identifierSource: KtSourceElement? = null
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        var typeArgumentsSource: KtSourceElement? = null
        userType.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.USER_TYPE -> simpleFirUserType = convertUserType(typeRefSource, it) as? FirUserTypeRef //simple user type
                KtNodeTypeId.REFERENCE_EXPRESSION -> {
                    identifierSource = it.toFirSourceElement()
                    identifier = it.asText
                }
                KtNodeTypeId.TYPE_ARGUMENT_LIST -> {
                    typeArgumentsSource = it.toFirSourceElement()
                    firTypeArguments += convertTypeArguments(it, allowedUnderscoredTypeArgument = false)
                }
            }
        }

        if (identifier == null) {
            return buildErrorTypeRef {
                source = typeRefSource
                diagnostic = ConeSyntaxDiagnostic("Incomplete user type")
                simpleFirUserType?.let { qualifierPart ->
                    if (qualifierPart.qualifier.isNotEmpty()) {
                        partiallyResolvedTypeRef = buildUserTypeRef {
                            source = qualifierPart.qualifier.last().source!!
                            isMarkedNullable = false
                            this.qualifier.addAll(qualifierPart.qualifier)
                        }
                    }
                }
            }
        }

        val qualifierPart = FirQualifierPartImpl(
            identifierSource!!,
            identifier.nameAsSafeName(),
            FirTypeArgumentListImpl(typeArgumentsSource ?: typeRefSource).apply {
                typeArguments += firTypeArguments
            }
        )

        return buildUserTypeRef {
            source = typeRefSource
            isMarkedNullable = isNullable
            qualifier.add(qualifierPart)
            simpleFirUserType?.qualifier?.let { this.qualifier.addAll(0, it) }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentList
     */
    fun convertTypeArguments(typeArguments: Node, allowedUnderscoredTypeArgument: Boolean): List<FirTypeProjection> {
        return typeArguments.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypeId.TYPE_PROJECTION -> container += convertTypeProjection(node, allowedUnderscoredTypeArgument)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.tryParseTypeArgumentList
     */
    private fun convertTypeProjection(typeProjection: Node, allowedUnderscoredTypeArgument: Boolean): FirTypeProjection {
        var modifiers: TypeProjectionModifierList<Node>? = null
        lateinit var firType: FirTypeRef
        var isStarProjection = false
        typeProjection.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.MODIFIER_LIST -> modifiers = convertTypeArgumentModifierList(it)
                KtNodeTypeId.TYPE_REFERENCE -> firType = convertType(it)
                KtTokenId.MUL -> isStarProjection = true
            }
        }

        //annotations from modifiers must be ignored
        return when {
            isStarProjection -> buildStarProjection { source = typeProjection.toFirSourceElement() }
            allowedUnderscoredTypeArgument && (firType as? FirUserTypeRef)?.isUnderscored == true -> buildPlaceholderProjection {
                source = typeProjection.toFirSourceElement()
            }
            else -> buildTypeProjectionWithVariance {
                source = typeProjection.toFirSourceElement()
                typeRef = firType
                variance = (modifiers ?: TypeProjectionModifierList()).getVariance()
            }
        }
    }

    val FirUserTypeRef.isUnderscored: Boolean
        get() {
            val qualifierSource = qualifier.lastOrNull()?.source ?: return false
            val text = qualifierSource.toNode().getChildNodeByTokenId(KtTokenId.IDENTIFIER)?.asText
            return text == "_"
        }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionType
     */
    private fun convertFunctionType(
        typeRefSource: KtSourceElement,
        functionType: Node,
        allTypeModifiers: List<ModifierList<Node>>,
        isNullable: Boolean = false,
    ): FirTypeRef {
        var receiverTypeReference: FirTypeRef? = null
        lateinit var returnTypeReference: FirTypeRef
        val parameters = mutableListOf<FirFunctionTypeParameter>()
        var contextList: Node? = null
        functionType.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.FUNCTION_TYPE_RECEIVER -> receiverTypeReference = convertReceiverType(it)
                KtNodeTypeId.VALUE_PARAMETER_LIST -> parameters += convertFunctionTypeParameters(it)
                KtNodeTypeId.TYPE_REFERENCE -> returnTypeReference = convertType(it)
                KtNodeTypeId.CONTEXT_PARAMETER_LIST -> contextList = it
            }
        }

        return buildFunctionTypeRef {
            source = typeRefSource
            isMarkedNullable = isNullable
            receiverTypeRef = receiverTypeReference
            returnTypeRef = returnTypeReference
            this.parameters += parameters
            isSuspend = allTypeModifiers.any { it.hasSuspend() }

            contextList?.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypeId.CONTEXT_RECEIVER, KtNodeTypeId.VALUE_PARAMETER -> {
                        val typeReference = it.getChildNodeByTokenId(KtNodeTypeId.TYPE_REFERENCE)

                        contextParameterTypeRefs += typeReference?.let(::convertType)
                            ?: buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Type missing") }
                    }
                }
            }
        }
    }

    private fun convertFunctionTypeParameters(
        parameters: Node,
    ): List<FirFunctionTypeParameter> {
        return parameters.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypeId.VALUE_PARAMETER -> {
                    var name: Name? = null
                    var typeRef: FirTypeRef? = null
                    node.forEachChildren {
                        when (it.toTokenId()) {
                            KtTokenId.IDENTIFIER -> name = it.asText.nameAsSafeName()
                            KtNodeTypeId.TYPE_REFERENCE -> typeRef = convertType(it)
                        }
                    }
                    container += buildFunctionTypeParameter {
                        val parameterSource = node.toFirSourceElement()
                        source = parameterSource
                        this.name = name
                        this.returnTypeRef = typeRef ?: createNoTypeForParameterTypeRef(parameterSource)
                    }
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameterList
     */
    fun convertValueParameters(
        valueParameters: Node,
        functionSymbol: FirFunctionSymbol<*>,
        valueParameterDeclaration: ValueParameterDeclaration,
        additionalAnnotations: List<FirAnnotation> = emptyList()
    ): List<ValueParameter<Node>> {
        return valueParameters.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypeId.VALUE_PARAMETER -> container += convertValueParameter(
                    node,
                    functionSymbol,
                    valueParameterDeclaration,
                    additionalAnnotations
                )
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameter
     */
    fun convertValueParameter(
        valueParameter: Node,
        containingDeclarationSymbol: FirBasedSymbol<*>?,
        valueParameterDeclaration: ValueParameterDeclaration,
        additionalAnnotations: List<FirAnnotation> = emptyList()
    ): ValueParameter<Node> {
        var modifiers: ModifierList<Node>? = null
        var isVal = false
        var isVar = false
        var identifier: String? = null
        var firType: FirTypeRef? = null
        var firExpression: FirExpression? = null
        var destructuringDeclaration: DestructuringDeclaration? = null
        valueParameter.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.MODIFIER_LIST -> modifiers = convertModifierList(it)
                KtTokenId.VAL_KEYWORD -> isVal = true
                KtTokenId.VAR_KEYWORD -> isVar = true
                KtTokenId.IDENTIFIER -> identifier = it.asText
                KtNodeTypeId.TYPE_REFERENCE -> {}
                KtNodeTypeId.DESTRUCTURING_DECLARATION -> destructuringDeclaration = convertDestructingDeclaration(it)
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have default value")
            }
        }

        val name = convertValueParameterName(identifier.nameAsSafeName(), valueParameterDeclaration) { identifier }
        val valueParameterSymbol = FirValueParameterSymbol()
        context.withContainerSymbol(valueParameterSymbol, isLocal = !valueParameterDeclaration.isAnnotationOwner) {
            valueParameter.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypeId.TYPE_REFERENCE -> firType = convertType(it)
                }
            }

            val valueParameterSource = valueParameter.toFirSourceElement()
            return ValueParameter(
                valueParameterSymbol = valueParameterSymbol,
                isVal = isVal,
                isVar = isVar,
                modifiers = modifiers ?: ModifierList(),
                valueParameterAnnotations = modifiers?.convertAnnotations() ?: emptyList(),
                returnTypeRef = firType
                    ?: when {
                        valueParameterDeclaration.shouldExplicitParameterTypeBePresent -> createNoTypeForParameterTypeRef(
                            valueParameterSource
                        )
                        else -> implicitType
                    },
                source = valueParameterSource,
                moduleData = baseModuleData,
                isFromPrimaryConstructor = valueParameterDeclaration == ValueParameterDeclaration.PRIMARY_CONSTRUCTOR,
                isContextParameter = valueParameterDeclaration == ValueParameterDeclaration.CONTEXT_PARAMETER,
                additionalAnnotations = additionalAnnotations,
                name = name,
                defaultValue = firExpression,
                containingDeclarationSymbol = containingDeclarationSymbol,
                destructuringDeclaration = destructuringDeclaration
            )
        }
    }

    private fun <T> fillDanglingConstraintsTo(
        typeParameters: List<FirTypeParameter>,
        typeConstraints: List<TypeConstraint>,
        to: T
    ) where T : FirDeclaration, T : FirTypeParameterRefsOwner {
        val typeParamNames = typeParameters.map { it.name }.toSet()
        val result = typeConstraints.mapNotNull { constraint ->
            val name = constraint.identifier?.nameAsSafeName()
            if (name != null && !typeParamNames.contains(name)) {
                DanglingTypeConstraint(name, constraint.source)
            } else {
                null
            }

        }
        if (result.isNotEmpty()) {
            to.danglingTypeConstraints = result
        }
    }

    private fun MutableList<FirValueParameter>.addContextParameters(
        contextLists: List<Node>?,
        containingDeclarationSymbol: FirBasedSymbol<*>,
    ) {
        if (contextLists == null) return
        for (contextList in contextLists) {
            contextList.getChildNodesByTokenId(KtNodeTypeId.VALUE_PARAMETER).mapTo(this) { contextParameterElement ->
                convertValueParameter(
                    valueParameter = contextParameterElement,
                    containingDeclarationSymbol = containingDeclarationSymbol,
                    valueParameterDeclaration = ValueParameterDeclaration.CONTEXT_PARAMETER
                ).firValueParameter
            }

            // Legacy context receivers
            contextList.getChildNodesByTokenId(KtNodeTypeId.CONTEXT_RECEIVER).mapTo(this) { contextReceiverElement ->
                buildValueParameter {
                    this.source = contextReceiverElement.toFirSourceElement()
                    this.moduleData = baseModuleData
                    this.origin = FirDeclarationOrigin.Source

                    val customLabelName =
                        contextReceiverElement
                            .getChildNodeByTokenId(KtNodeTypeId.LABEL_QUALIFIER)
                            ?.getChildNodeByTokenId(KtNodeTypeId.LABEL)
                            ?.getChildNodeByTokenId(KtTokenId.IDENTIFIER)
                            ?.getReferencedNameAsName()

                    val typeReference = contextReceiverElement.getChildNodeByTokenId(KtNodeTypeId.TYPE_REFERENCE)

                    val labelNameFromTypeRef = typeReference?.getChildNodeByTokenId(KtNodeTypeId.USER_TYPE)
                        ?.getChildNodeByTokenId(KtNodeTypeId.REFERENCE_EXPRESSION)
                        ?.getReferencedNameAsName()

                    // We're abusing the value parameter name for the label/type name of legacy context receivers.
                    // Luckily, legacy context receivers are getting removed soon.
                    this.name = customLabelName ?: labelNameFromTypeRef ?: SpecialNames.UNDERSCORE_FOR_UNUSED_VAR

                    this.symbol = FirValueParameterSymbol()
                    context.withContainerSymbol(this.symbol) {
                        this.returnTypeRef = typeReference?.let { convertType(it) }
                            ?: buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Type missing") }
                    }
                    this.containingDeclarationSymbol = containingDeclarationSymbol
                    this.valueParameterKind = FirValueParameterKind.LegacyContextReceiver
                }
            }
        }
    }

    private fun Node.getReferencedNameAsName(): Name {
        return asText.nameAsSafeName()
    }

    override fun convertScript(
        script: Node,
        scriptSource: KtSourceElement,
        fileName: String,
        setup: FirScriptBuilder.() -> Unit,
    ): FirScript {
        val scriptName = firScriptName(fileName)
        val scriptSymbol = FirScriptSymbol(context.packageFqName.child(scriptName))

        return buildScript {
            source = scriptSource
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            name = scriptName
            symbol = scriptSymbol

            val childNodes = script.getChildNodeByTokenId(KtNodeTypeId.BLOCK)?.getChildren().orEmpty()
                .filter { it.toTokenId() in scriptDeclarationTokensId }

            val scriptDeclarationsIter = childNodes.listIterator()
            context.withContainerScriptSymbol(symbol) {
                val modifierLists = mutableListOf<Node>()
                while (scriptDeclarationsIter.hasNext()) {
                    val declarationSource = scriptDeclarationsIter.next()
                    val isLast = !scriptDeclarationsIter.hasNext()
                    when (declarationSource.toTokenId()) {
                        KtNodeTypeId.SCRIPT_INITIALIZER -> {
                            val initializer = convertScriptInitializer(
                                scriptInitializer = declarationSource,
                                containingDeclarationSymbol = scriptSymbol,
                                // the last anonymous initializer could be converted to a property, and its symbol will be dropped
                                // therefore we should not rely on it as a containing declaration symbol, and use the parent one instead
                                isLocal = isLast,
                            )

                            initializer.isScriptTopLevelDeclaration = true
                            declarations.add(initializer)
                        }

                        KtNodeTypeId.DESTRUCTURING_DECLARATION -> {
                            val destructuringDeclaration = convertDestructingDeclaration(declarationSource)
                            val destructuringContainerVar = generateTemporaryVariable(
                                moduleData = baseModuleData,
                                source = declarationSource.toFirSourceElement(),
                                name = SpecialNames.DESTRUCT,
                                initializer = destructuringDeclaration.initializer,
                                extractedAnnotations = destructuringDeclaration.annotations,
                                origin = FirDeclarationOrigin.Synthetic.DestructuringDeclarationContainer
                            ).apply {
                                isScriptTopLevelDeclaration = true
                                isDestructuringDeclarationContainerVariable = true
                            }
                            addDestructuringStatements(
                                declarations,
                                context,
                                baseModuleData,
                                destructuringDeclaration,
                                destructuringContainerVar,
                                isTmpVariable = true,
                                forceLocal = false,
                            ) {
                                configureNonLocalDestructuringDeclarationEntry(it, destructuringContainerVar)
                                it.isScriptTopLevelDeclaration = true
                            }
                        }

                        else -> {
                            convertDeclarationFromClassBody(declarationSource, declarations, classWrapper = null, modifierLists)
                            declarations.lastOrNull()?.isScriptTopLevelDeclaration = true
                        }
                    }
                }
                convertDanglingModifierListsInClassBody(modifierLists, declarations)
                setup()
            }
        }
    }

    override fun convertReplSnippet(
        script: Node,
        scriptSource: KtSourceElement,
        fileName: String,
        snippetSetup: FirReplSnippetBuilder.() -> Unit,
        functionBodySetup: FirBlockBuilder.() -> Unit,
        statementsSetup: MutableList<FirElement>.() -> Unit,
    ): FirReplSnippet = convertReplSnippetImpl(
        script = script,
        scriptSource = scriptSource,
        fileName = fileName,
        scopeProvider = baseScopeProvider,
        snippetSetup = snippetSetup,
        functionBodySetup = functionBodySetup,
        statementsSetup = statementsSetup,
        extractReplElements = { containingDeclarationSymbol, copiedDelegatedProperties ->
            extractReplElements(script, containingDeclarationSymbol, copiedDelegatedProperties)
        },
    )

    private fun extractReplElements(
        script: Node,
        containingDeclarationSymbol: FirRegularClassSymbol,
        copiedDelegatedProperties: MutableMap<FirPropertySymbol, FirProperty>,
    ): List<FirElement> {
        val childNodes = script.getChildNodeByTokenId(KtNodeTypeId.BLOCK)?.getChildren().orEmpty()
            .filter { it.toTokenId() in scriptDeclarationTokensId }

        val result = mutableListOf<FirElement>()
        val modifierLists = mutableListOf<Node>()
        for (declarationSource in childNodes) {
            when (declarationSource.toTokenId()) {
                KtNodeTypeId.SCRIPT_INITIALIZER -> {
                    result += convertScriptInitializer(
                        scriptInitializer = declarationSource,
                        containingDeclarationSymbol = containingDeclarationSymbol,
                        isLocal = true,
                    )
                }

                KtNodeTypeId.DESTRUCTURING_DECLARATION -> {
                    val destructuringDeclaration = convertDestructingDeclaration(declarationSource)
                    val destructuringContainerVar = generateTemporaryVariable(
                        moduleData = baseModuleData,
                        source = declarationSource.toFirSourceElement(),
                        name = SpecialNames.DESTRUCT,
                        initializer = destructuringDeclaration.initializer,
                        extractedAnnotations = destructuringDeclaration.annotations,
                        origin = FirDeclarationOrigin.Source,
                    ).apply {
                        isDestructuringDeclarationContainerVariable = true
                    }
                    addDestructuringStatements(
                        result,
                        context,
                        baseModuleData,
                        destructuringDeclaration,
                        destructuringContainerVar,
                        isTmpVariable = true,
                        forceLocal = false,
                    ) {
                        configureNonLocalDestructuringDeclarationEntry(it, destructuringContainerVar)
                    }
                }

                KtNodeTypeId.PROPERTY -> {
                    val firProperty = convertPropertyDeclaration(
                        declarationSource,
                        classWrapper = null,
                        ownerRegularOrAnonymousObjectSymbol = containingDeclarationSymbol,
                    )

                    // See documentation on `replSnippetDelegatedPropertyCopies` attribute for why this is needed.
                    if (firProperty.delegate != null) {
                        val firPropertyCopy = convertPropertyDeclaration(
                            declarationSource,
                            classWrapper = null,
                            ownerRegularOrAnonymousObjectSymbol = containingDeclarationSymbol,
                        )

                        @OptIn(FirImplementationDetail::class)
                        firPropertyCopy.isCopiedDelegatedProperty = true
                        copiedDelegatedProperties[firProperty.symbol] = firPropertyCopy
                    }

                    result += firProperty
                }

                else -> {
                    val declarations = mutableListOf<FirDeclaration>()
                    convertDeclarationFromClassBody(declarationSource, declarations, classWrapper = null, modifierLists)
                    result += declarations
                }
            }
        }
        val danglingModifierLists = mutableListOf<FirDeclaration>()
        convertDanglingModifierListsInClassBody(modifierLists, danglingModifierLists)
        result += danglingModifierLists
        return result
    }

    override fun Node.toFirSourceElement(kind: KtFakeSourceElementKind?): KtSourceElement {
        return with(rootBuilder) { toFirSourceElement(kind) }
    }

    override fun KtSourceElement.toNode(): Node {
        return with(rootBuilder) { toNode() }
    }

    override val Node.elementType: Type
        get() = with(rootBuilder) { elementType }
    override val Node.asText: String
        get() = with(rootBuilder) { asText }

    override val Node.receiverExpression: Node?
        get() = with(rootBuilder) { receiverExpression }
    override val Node.selectorExpression: Node?
        get() = with(rootBuilder) { selectorExpression }

    override fun Type.typeToTokenId(): Int {
        return with(rootBuilder) { typeToTokenId() }
    }

    override fun Node.getParent(): Node? {
        return with(rootBuilder) { getParent() }
    }

    override fun Node.getChildren(): List<Node> {
        return with(rootBuilder) { getChildren() }
    }

    override val Node.indexExpressions: List<Node>?
        get() = with(rootBuilder) { indexExpressions }

    override fun KtSourceElement.isChildInParentheses(): Boolean {
        return with(rootBuilder) { isChildInParentheses()}
    }

    override fun Node.getChildrenAsArray(): Array<out Node?> {
        return with(rootBuilder) { getChildrenAsArray() }
    }

    private fun Node.isExpression(): Boolean = toTokenId().isExpression()

    private val scriptDeclarationTokensId = setOf(
        KtNodeTypeId.CLASS, KtNodeTypeId.FUNCTION, KtNodeTypeId.PROPERTY,
        KtNodeTypeId.TYPEALIAS, KtNodeTypeId.OBJECT_DECLARATION, KtNodeTypeId.CLASS_INITIALIZER,
        KtNodeTypeId.MODIFIER_LIST, KtNodeTypeId.SCRIPT_INITIALIZER, KtNodeTypeId.DESTRUCTURING_DECLARATION
    )
}
