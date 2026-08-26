/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.NodeTypeAnalyzer
import org.jetbrains.kotlin.fir.analysis.firstFunctionCallInBlockHasLambdaArgumentWithLabel
import org.jetbrains.kotlin.fir.analysis.isCallTheFirstStatement
import org.jetbrains.kotlin.fir.analysis.isExpression
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder.Companion.firScriptName
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirAnonymousFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirNamedFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirPrimaryConstructorBuilder
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
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class TreeRawFirDeclarationBuilderProxy<Node : Any, Type : Any>(
    val analyzer: NodeTypeAnalyzer<Node, Type>,
    val context: Context<Node>,
    val baseModuleData: FirModuleData,
    val expressionConverter: TreeRawFirExpressionBuilderProxy<Node, Type>,
    val headerMode: Boolean,
    val baseScopeProvider: FirScopeProvider,
) : TreeDeclarationConverter<Node>, NodeTypeAnalyzer<Node, Type> by analyzer {
    /**
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parseFile]
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parsePreamble]
     */
    fun convertFile(file: Node, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping): FirFile {
        if (file.toTokenId() != KtNodeTypes.FILE_ID) {
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
                KtNodeTypes.FILE_ANNOTATION_LIST_ID -> {
                    context.withContainerSymbol(fileSymbol) {
                        convertAnnotationsOnlyTo(child, fileAnnotations)
                    }
                }
                KtNodeTypes.PACKAGE_DIRECTIVE_ID -> {
                    packageDirective = convertPackageDirective(child).also { context.packageFqName = it.packageFqName }
                }
                KtNodeTypes.IMPORT_LIST_ID -> importList += convertImportDirectives(child)
                KtNodeTypes.CLASS_ID, KtNodeTypes.OBJECT_DECLARATION_ID -> firDeclarationList += convertClass(child)
                KtNodeTypes.FUNCTION_ID -> firDeclarationList += convertFunctionDeclaration(child) as FirDeclaration
                KtNodeTypes.PROPERTY_ID -> firDeclarationList += convertPropertyDeclaration(child)
                KtNodeTypes.TYPEALIAS_ID -> firDeclarationList += convertTypeAlias(child)
                KtNodeTypes.DESTRUCTURING_DECLARATION_ID -> {
                    val initializer = buildFirDestructuringDeclarationInitializer(child)
                    firDeclarationList += buildErrorNonLocalDestructuringDeclaration(
                        child.toFirSourceElement(), initializer, baseModuleData
                    )
                }
                KtNodeTypes.SCRIPT_ID -> scriptNodes += child
                KtNodeTypes.MODIFIER_LIST_ID -> modifierList += child
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
    override fun convertBlockExpression(
        node: Node,
        convertOnlyFirstStatement: Boolean,
    ): FirBlock {
        return convertBlockExpressionWithoutBuilding(node, convertOnlyFirstStatement = convertOnlyFirstStatement).build()
    }

    /**
     * @param convertOnlyFirstStatement Convert only the first statement of the block, which can be a contract, for header generation.
     */
    override fun convertBlockExpressionWithoutBuilding(
        block: Node,
        kind: KtFakeSourceElementKind?,
        convertOnlyFirstStatement: Boolean,
    ): FirBlockBuilder {
        val firStatements = block.forEachChildrenReturnList { node, container ->
            if (!convertOnlyFirstStatement || container.isEmpty()) {
                when (node.toTokenId()) {
                    KtNodeTypes.CLASS_ID, KtNodeTypes.OBJECT_DECLARATION_ID -> container += convertClass(node) as FirStatement
                    KtNodeTypes.FUNCTION_ID -> container += convertFunctionDeclaration(node)
                    KtNodeTypes.PROPERTY_ID -> container += convertPropertyDeclaration(node) as FirStatement
                    KtNodeTypes.DESTRUCTURING_DECLARATION_ID -> container +=
                        convertDestructingDeclaration(node).toFirDestructingDeclaration(this, context, baseModuleData)
                    KtNodeTypes.TYPEALIAS_ID -> container += convertTypeAlias(node) as FirStatement
                    KtNodeTypes.CLASS_INITIALIZER_ID -> shouldNotBeCalled("CLASS_INITIALIZER expected to be processed during class body conversion")
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
                KtNodeTypes.DOT_QUALIFIED_EXPRESSION_ID, KtNodeTypes.REFERENCE_EXPRESSION_ID -> packageName = parsePackageName(it)
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
            if (node.toTokenId() == KtNodeTypes.DOT_QUALIFIED_EXPRESSION_ID) {
                val children = node.getChildren()

                if (children.size == 3) {
                    return parse(children.first()).apply {
                        add(children.last().getAsStringWithoutBacktick())
                    }
                }
            }

            if (node.toTokenId() == KtNodeTypes.REFERENCE_EXPRESSION_ID) {
                return mutableListOf(node.getAsStringWithoutBacktick())
            }

            return mutableListOf()
        }

        return parse(node)
    }

    private fun convertImportAlias(importAlias: Node): Pair<String, KtSourceElement>? {
        var result: Pair<String, KtSourceElement>? = null
        importAlias.forEachChildren {
            if (result != null) return@forEachChildren
            when (it.toTokenId()) {
                KtTokens.IDENTIFIER_ID -> result = Pair(it.asText, it.toFirSourceElement())
            }
        }

        return result
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
                KtNodeTypes.REFERENCE_EXPRESSION_ID, KtNodeTypes.DOT_QUALIFIED_EXPRESSION_ID -> {
                    importedFqName = mutableListOf<String>()
                        .apply { collectSegments(child) }
                        .joinToString(".")
                        .let { FqName(it) }
                }
                KtTokens.MUL_ID -> isAllUnder = true
                KtNodeTypes.IMPORT_ALIAS_ID -> {
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
            KtNodeTypes.REFERENCE_EXPRESSION_ID -> add(expression.getAsStringWithoutBacktick())
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION_ID -> {
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
                KtNodeTypes.IMPORT_DIRECTIVE_ID -> container += convertImportDirective(node)
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
                KtNodeTypes.ANNOTATION_ID -> annotations += it
                KtNodeTypes.ANNOTATION_ENTRY_ID -> annotations += it
                KtNodeTypes.CONTEXT_PARAMETER_LIST_ID -> contextLists += it
                KtTokens.IN_MODIFIER_ID, KtTokens.FUN_MODIFIER_ID -> addModifier(it.toTokenId(), isInClass)
                in KtTokens.ABSTRACT_MODIFIER_ID..KtTokens.ACTUAL_MODIFIER_ID -> addModifier(it.toTokenId(), isInClass)
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
            KtNodeTypes.ANNOTATION_ID -> convertAnnotationTo(node, list)
            KtNodeTypes.ANNOTATION_ENTRY_ID -> list += convertAnnotationEntry(node)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseAnnotationOrList
     */
    override fun convertAnnotationTo(node: Node, list: MutableList<in FirAnnotationCall>) {
        var annotationTarget: AnnotationUseSiteTarget? = null
        node.forEachChildren { child ->
            when (child.toTokenId()) {
                KtNodeTypes.ANNOTATION_TARGET_ID -> annotationTarget = convertAnnotationTarget(child)
                KtNodeTypes.ANNOTATION_ENTRY_ID -> list += convertAnnotationEntry(
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
                KtTokens.ALL_KEYWORD_ID -> annotationTarget = ALL
                KtTokens.FIELD_KEYWORD_ID -> annotationTarget = FIELD
                KtTokens.FILE_KEYWORD_ID -> annotationTarget = FILE
                KtTokens.PROPERTY_KEYWORD_ID -> annotationTarget = PROPERTY
                KtTokens.GET_KEYWORD_ID -> annotationTarget = PROPERTY_GETTER
                KtTokens.SET_KEYWORD_ID -> annotationTarget = PROPERTY_SETTER
                KtTokens.RECEIVER_KEYWORD_ID -> annotationTarget = RECEIVER
                KtTokens.PARAM_KEYWORD_ID -> annotationTarget = CONSTRUCTOR_PARAMETER
                KtTokens.SETPARAM_KEYWORD_ID -> annotationTarget = SETTER_PARAMETER
                KtTokens.DELEGATE_KEYWORD_ID -> annotationTarget = PROPERTY_DELEGATE_FIELD
            }
        }

        return annotationTarget
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseAnnotation
     * can be treated as unescapedAnnotation
     */
    override fun convertAnnotationEntry(
        node: Node,
        defaultAnnotationUseSiteTarget: AnnotationUseSiteTarget?,
        diagnostic: ConeDiagnostic?,
    ): FirAnnotationCall = context.withForcedLocalContext {
        var annotationUseSiteTarget: AnnotationUseSiteTarget? = null
        lateinit var constructorCalleePair: Pair<FirTypeRef, List<FirExpression>>
        node.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.ANNOTATION_TARGET_ID -> annotationUseSiteTarget = convertAnnotationTarget(it)
                KtNodeTypes.CONSTRUCTOR_CALLEE_ID -> constructorCalleePair = convertConstructorInvocation(node)
            }
        }
        val qualifier = (constructorCalleePair.first as? FirUserTypeRef)?.qualifier?.last()
        val name = qualifier?.name ?: Name.special("<no-annotation-name>")
        val theCalleeReference = buildSimpleNamedReference {
            source = node
                .getChildNodeByTokenId(KtNodeTypes.CONSTRUCTOR_CALLEE_ID)
                ?.getChildNodeByTokenId(KtNodeTypes.TYPE_REFERENCE_ID)
                ?.getChildNodeByTokenId(KtNodeTypes.USER_TYPE_ID)
                ?.getChildNodeByTokenId(KtNodeTypes.REFERENCE_EXPRESSION_ID)
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
        return getChildNodesByTokenId(KtNodeTypes.VALUE_PARAMETER_LIST_ID).let {
            it.isNotEmpty() && it.first().getChildNodesByTokenId(KtNodeTypes.VALUE_PARAMETER_ID).isNotEmpty()
        }
    }

    /*****    DECLARATIONS    *****/
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassOrObject
     */
    override fun convertClass(node: Node): FirRegularClass {
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
                KtNodeTypes.MODIFIER_LIST_ID -> modifiers = convertModifierList(it, isInClass = true)
                KtTokens.IDENTIFIER_ID -> identifier = it.asText
            }
        }

        val calculatedModifiers = modifiers ?: ModifierList()
        val className = identifier.nameAsSafeName(if (calculatedModifiers.isCompanion()) "Companion" else "")
        val isLocalWithinParent = node.getParent()?.toTokenId() != KtNodeTypes.CLASS_BODY_ID && isClassLocal(node) { getParent() }
        val classIsExpect = calculatedModifiers.hasExpect() || context.containerIsExpect

        return context.withChildClassName(className, isExpect = classIsExpect, isLocalWithinParent) {
            val classSymbol = FirRegularClassSymbol(context.currentClassId)
            context.withContainerSymbol(classSymbol) {
                node.forEachChildren {
                    when (it.toTokenId()) {
                        KtTokens.CLASS_KEYWORD_ID -> classKind = ClassKind.CLASS
                        KtTokens.INTERFACE_KEYWORD_ID -> classKind = ClassKind.INTERFACE
                        KtTokens.OBJECT_KEYWORD_ID -> classKind = ClassKind.OBJECT
                        KtNodeTypes.TYPE_PARAMETER_LIST_ID -> typeParameterList = it
                        KtNodeTypes.PRIMARY_CONSTRUCTOR_ID -> primaryConstructor = it
                        KtNodeTypes.SUPER_TYPE_LIST_ID -> superTypeList = it
                        KtNodeTypes.TYPE_CONSTRAINT_LIST_ID -> typeConstraints += convertTypeConstraints(it)
                        KtNodeTypes.CLASS_BODY_ID -> classBody = it
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
                    isValue = calculatedModifiers.isValueClass()
                    isFun = calculatedModifiers.isFunctionalInterface()
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
                                delegatedSuperTypeRef = implicitAnyType
                            }
                        }

                        val classIsKotlinAny = symbol.classId == StandardClassIds.Any

                        if (superTypeRefs.isEmpty() && !classIsKotlinAny) {
                            val classIsKotlinNothing = symbol.classId == StandardClassIds.Nothing
                            // kotlin.Nothing doesn't have `Any` supertype, but does have delegating constructor call to Any
                            if (!classIsKotlinNothing) {
                                superTypeRefs += implicitAnyType
                            }
                            delegatedSuperTypeRef = implicitAnyType
                        }

                        this.superTypeRefs += superTypeRefs

                        val secondaryConstructors = classBody.getChildNodesByTokenId(KtNodeTypes.SECONDARY_CONSTRUCTOR_ID)
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
                            generateDataClassMembers(
                                primaryConstructor ?: node,
                                this,
                                firPrimaryConstructor,
                                zippedParameters,
                                context.packageFqName,
                                context.className,
                                addValueParameterAnnotations = { valueParam ->
                                    context.withContainerSymbol(symbol) {
                                        valueParam.forEachChildren { node ->
                                            if (node.toTokenId() == KtNodeTypes.MODIFIER_LIST_ID) {
                                                buildList {
                                                    convertAnnotationsOnlyTo(node, this)
                                                }.filterTo(annotations) {
                                                    it.useSiteTarget.appliesToPrimaryConstructorParameter()
                                                }
                                            }
                                        }
                                    }
                                },
                            )
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
            if (node.getParent()?.toTokenId() == KtNodeTypes.CLASS_BODY_ID) {
                it.initContainingClassForLocalAttr()
            }
            it.initContainingScriptOrReplAttr()
            if (isDirectlyInsideCompanionBlock) {
                it.isIllegalCompanionBlockMember = true
            }
        }
    }

    /**
     * see PsiRawFirBuilder.Visitor.visitObjectLiteralExpression
     *
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseObjectLiteral
     */
    override fun convertObjectLiteral(node: Node): FirAnonymousObjectExpression {
        return context.withChildClassName(SpecialNames.ANONYMOUS, forceLocalContext = true, isExpect = false) {
            var delegatedFieldsMap: Map<Int, FirFieldSymbol>? = null
            buildAnonymousObjectExpression {
                source = node.toFirSourceElement()

                val objectDeclaration = node.getChildNodesByTokenId(KtNodeTypes.OBJECT_DECLARATION_ID).first()
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
                        KtNodeTypes.MODIFIER_LIST_ID -> {
                            modifiers = convertModifierList(child)
                        }
                        KtNodeTypes.PRIMARY_CONSTRUCTOR_ID -> primaryConstructor = child
                        KtNodeTypes.SUPER_TYPE_LIST_ID -> convertDelegationSpecifiers(child).let { specifiers ->
                            delegatedSuperTypeRef = specifiers.superTypeCalls.lastOrNull()?.delegatedSuperTypeRef
                            superTypeRefs += specifiers.superTypesRef
                            delegatedConstructorSource = specifiers.superTypeCalls.lastOrNull()?.source
                            delegateFields = specifiers.delegateFieldsMap.values.map { it.fir }
                            delegatedFieldsMap = specifiers.delegateFieldsMap.takeIf { it.isNotEmpty() }
                            delegatedSuperCalls = specifiers.superTypeCalls
                        }
                        KtNodeTypes.CLASS_BODY_ID -> classBody = child
                    }
                }
                val companionBlockCollector = CompanionBlockCollector()
                anonymousObject = buildAnonymousObject {
                    source = objectDeclaration.toFirSourceElement()
                    origin = FirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    classKind = ClassKind.CLASS
                    scopeProvider = baseScopeProvider
                    symbol = FirAnonymousObjectSymbol(context.packageFqName)
                    status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                    context.appendOuterTypeParameters(ignoreLastLevel = false, typeParameters)
                    val delegatedSelfType = objectDeclaration.toDelegatedSelfType(this)
                    registerSelfType(delegatedSelfType)

                    superTypeRefs.ifEmpty {
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
                        hasSecondaryConstructor = classBody.getChildNodesByTokenId(KtNodeTypes.SECONDARY_CONSTRUCTOR_ID).isNotEmpty(),
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

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumEntry
     */
    private fun convertEnumEntry(enumEntry: Node, classWrapper: ClassWrapper<Node>): FirEnumEntry {
        var modifiers: ModifierList<Node>? = null
        lateinit var identifier: String
        val enumSuperTypeCallEntry = mutableListOf<FirExpression>()
        var classBodyNode: Node? = null
        var superTypeCallEntry: Node? = null
        enumEntry.getChildNodeByTokenId(KtTokens.IDENTIFIER_ID)?.let {
            identifier = it.asText
        }

        val enumEntryName = identifier.nameAsSafeName()
        val containingClassIsExpectClass = classWrapper.hasExpect() || context.containerIsExpect
        return buildEnumEntry {
            symbol = FirEnumEntrySymbol(CallableId(context.currentClassId, enumEntryName))
            context.withContainerSymbol(symbol) {
                enumEntry.forEachChildren {
                    when (it.toTokenId()) {
                        KtNodeTypes.MODIFIER_LIST_ID -> {
                            modifiers = convertModifierList(it)
                        }
                        KtNodeTypes.INITIALIZER_LIST_ID -> {
                            enumSuperTypeCallEntry += convertInitializerList(it)
                            it.getChildNodeByTokenId(KtNodeTypes.SUPER_TYPE_CALL_ENTRY_ID)?.let { superTypeCall ->
                                superTypeCallEntry = superTypeCall
                            }
                        }
                        KtNodeTypes.CLASS_BODY_ID -> classBodyNode = it
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
                if (classWrapper.hasDefaultConstructor && enumEntry.getChildNodeByTokenId(KtNodeTypes.INITIALIZER_LIST_ID) == null &&
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
                                hasSecondaryConstructor = classBodyNode.getChildNodesByTokenId(KtNodeTypes.SECONDARY_CONSTRUCTOR_ID).isNotEmpty(),
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
                KtNodeTypes.SUPER_TYPE_CALL_ENTRY_ID -> convertConstructorInvocation(it).apply {
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
            KtNodeTypes.ENUM_ENTRY_ID -> container += convertEnumEntry(node, classWrapper!!)
            KtNodeTypes.CLASS_ID, KtNodeTypes.OBJECT_DECLARATION_ID -> container += convertClass(node)
            KtNodeTypes.FUNCTION_ID -> container += convertFunctionDeclaration(node) as FirDeclaration
            KtNodeTypes.PROPERTY_ID -> container += convertPropertyDeclaration(node, classWrapper)
            KtNodeTypes.TYPEALIAS_ID -> container += convertTypeAlias(node)
            KtNodeTypes.CLASS_INITIALIZER_ID -> convertAnonymousInitializer(
                node, classWrapper!!.classBuilder.ownerRegularOrAnonymousObjectSymbol
            )?.also {
                container += it
            } //anonymousInitializer
            KtNodeTypes.SECONDARY_CONSTRUCTOR_ID -> container += convertSecondaryConstructor(node, classWrapper!!)
            KtNodeTypes.MODIFIER_LIST_ID -> modifierLists += node
            KtNodeTypes.DESTRUCTURING_DECLARATION_ID -> {
                val initializer = buildFirDestructuringDeclarationInitializer(node)
                container += buildErrorNonLocalDestructuringDeclaration(node.toFirSourceElement(), initializer, baseModuleData)
            }
            KtNodeTypes.COMPANION_BLOCK_ID -> {
                classWrapper?.companionBlockCollector?.collect(node.toFirSourceElement(), isNested = isDirectlyInsideCompanionBlock)
                context.withCompanionBlock {
                    node.getChildNodeByTokenId(KtNodeTypes.CLASS_BODY_ID)?.let { container.addAll(convertClassBody(it, classWrapper)) }
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

    private fun buildFirDestructuringDeclarationInitializer(destructuringDeclaration: Node): FirExpression {
        val initializer = destructuringDeclaration.getFirstChildExpression().takeUnless {
            it?.toTokenId() == KtNodeTypes.PROPERTY_DELEGATE_ID
        }
        return expressionConverter.getAsFirExpression(
            initializer,
            "Initializer required for destructuring declaration",
            sourceWhenInvalidExpression = destructuringDeclaration
        )
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
                    KtNodeTypes.MODIFIER_LIST_ID -> {
                        modifiersIfPresent = convertModifierList(it)
                    }
                    KtNodeTypes.VALUE_PARAMETER_LIST_ID -> valueParameters += convertValueParameters(
                        it,
                        constructorSymbol,
                        NodeTypeAnalyzer.ValueParameterDeclaration.PRIMARY_CONSTRUCTOR
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
                                    ?.getChildNodeByTokenId(KtNodeTypes.CONSTRUCTOR_CALLEE_ID)
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
                    KtNodeTypes.MODIFIER_LIST_ID -> convertAnnotationsOnlyTo(it, annotations)
                    KtNodeTypes.BLOCK_ID -> context.withForcedLocalContext {
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
                    KtNodeTypes.MODIFIER_LIST_ID -> {
                        modifiers = convertModifierList(it)
                    }
                    KtNodeTypes.VALUE_PARAMETER_LIST_ID -> firValueParameters += convertValueParameters(
                        it,
                        constructorSymbol,
                        NodeTypeAnalyzer.ValueParameterDeclaration.FUNCTION
                    )
                    KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL_ID -> delegatedConstructorNode = it
                    KtNodeTypes.BLOCK_ID -> block = it
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
                KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE_ID -> if (it.asText == "this") thisKeywordPresent = true
                KtNodeTypes.VALUE_ARGUMENT_LIST_ID -> firValueArguments += expressionConverter.convertValueArguments(it)
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
            val calleeSource = constructorDelegationCall.getChildNodeByTokenId(KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE_ID)
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
    override fun convertTypeAlias(node: Node): FirTypeAlias {
        var modifiers: ModifierList<Node>? = null
        var identifier: String? = null
        lateinit var typeRefNode: Node
        var typeParametersNode: Node? = null

        node.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.MODIFIER_LIST_ID -> modifiers = convertModifierList(it)
                KtTokens.IDENTIFIER_ID -> identifier = it.asText
                KtNodeTypes.TYPE_REFERENCE_ID -> typeRefNode = it
                KtNodeTypes.TYPE_PARAMETER_LIST_ID -> typeParametersNode = it
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
            if (node.getParent()?.toTokenId() == KtNodeTypes.CLASS_BODY_ID) {
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
    override fun convertPropertyDeclaration(node: Node, classWrapper: ClassWrapper<Node>?): FirProperty {
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
        node.getChildNodeByTokenId(KtTokens.IDENTIFIER_ID)?.let {
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
                    KtNodeTypes.MODIFIER_LIST_ID -> {
                        modifiers = convertModifierList(it)
                    }
                    KtNodeTypes.TYPE_PARAMETER_LIST_ID -> typeParameterList = it
                    KtTokens.COLON_ID -> isReturnType = true
                    KtNodeTypes.TYPE_REFERENCE_ID -> if (isReturnType) returnType = convertType(it) else receiverTypeNode = it
                    KtNodeTypes.TYPE_CONSTRAINT_LIST_ID -> typeConstraints += convertTypeConstraints(it)
                    KtNodeTypes.PROPERTY_DELEGATE_ID -> delegate = it
                    KtTokens.VAR_KEYWORD_ID -> isVar = true
                    KtNodeTypes.PROPERTY_ACCESSOR_ID -> {
                        accessors += it
                    }
                    KtNodeTypes.BACKING_FIELD_ID -> fieldDeclaration = it
                    else -> if (it.isExpression()) {
                        context.calleeNamesForLambda += null
                        val keepBodyInHeaderMode = !isReturnType || modifiers?.isConst() == true
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
                        classWrapper?.classBuilder?.ownerRegularOrAnonymousObjectSymbol,
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
                            runUnless(isStatic) { classWrapper?.classBuilder?.ownerRegularOrAnonymousObjectSymbol },
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
    override fun convertDestructingDeclaration(node: Node): DestructuringDeclaration {
        val annotations = mutableListOf<FirAnnotationCall>()
        var isVar = false
        var hasSquareBrackets = false
        val entries = mutableListOf<DestructuringEntry>()
        val source = node.toFirSourceElement()
        var firExpression: FirExpression? = null
        node.forEachChildren {
            when (it.toTokenId()) {
                KtTokens.LBRACKET_ID -> hasSquareBrackets = true
                KtNodeTypes.MODIFIER_LIST_ID -> convertAnnotationsOnlyTo(it, annotations)
                KtTokens.VAR_KEYWORD_ID -> isVar = true
                KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY_ID -> entries += convertDestructingDeclarationEntry(it, isVar)
                // Property delegates should be ignored as they aren't a valid initializer
                KtNodeTypes.PROPERTY_DELEGATE_ID -> {}
                else -> if (it.isExpression()) firExpression =
                    expressionConverter.getAsFirExpression(it, "Initializer required for destructuring declaration")
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
        val annotations = mutableListOf<FirAnnotationCall>()
        var identifier: String? = null
        var firType: FirTypeRef? = null
        var isVar = isVar
        var initializerName: Name? = null
        var initializerSource: KtSourceElement? = null
        var isFullForm = false
        entry.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.MODIFIER_LIST_ID -> convertAnnotationsOnlyTo(it, annotations)
                KtTokens.IDENTIFIER_ID -> identifier = it.asText
                KtNodeTypes.TYPE_REFERENCE_ID -> firType = convertType(it)
                KtTokens.VAL_KEYWORD_ID -> isFullForm = true
                KtTokens.VAR_KEYWORD_ID -> {
                    isFullForm = true
                    isVar = true
                }
                KtNodeTypes.REFERENCE_EXPRESSION_ID -> {
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

        return DestructuringEntry(
            source = entry.toFirSourceElement(),
            initializerSource = initializerSource,
            returnTypeRef = firType ?: implicitType,
            name = name,
            initializerName = initializerName,
            isVar = isVar,
            isFullForm = isFullForm,
            annotations = annotations,
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
                KtTokens.SET_KEYWORD_ID -> isGetter = false
                KtNodeTypes.MODIFIER_LIST_ID -> {
                    modifiers = convertModifierList(it)
                }
                KtNodeTypes.TYPE_REFERENCE_ID -> returnType = convertType(it)
                KtNodeTypes.VALUE_PARAMETER_LIST_ID -> {
                    // getter can have an empty value parameter list
                    if (!isGetter) {
                        firValueParameters = convertSetterParameter(
                            it, accessorSymbol, propertyTypeRefToUse, propertyAnnotations.filterUseSiteTarget(SETTER_PARAMETER)
                        )
                    }
                }
                KtNodeTypes.CONTRACT_EFFECT_LIST_ID -> outerContractDescription = obtainContractDescription(it)
                KtNodeTypes.BLOCK_ID -> block = it
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
                forceKeepingTheBodyInHeaderMode = propertyTypeRef is FirImplicitTypeRef || status.isInline
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
                it.toTokenId() == KtNodeTypes.MODIFIER_LIST_ID -> {
                    modifiers = convertModifierList(it)
                }
                it.toTokenId() == KtNodeTypes.TYPE_REFERENCE_ID -> returnType = convertType(it)
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
                KtNodeTypes.CONTRACT_EFFECT_ID -> {
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
                KtNodeTypes.MODIFIER_LIST_ID -> modifiers = convertModifierList(it)
                KtNodeTypes.VALUE_PARAMETER_ID -> firValueParameter =
                    convertValueParameter(it, functionSymbol, NodeTypeAnalyzer.ValueParameterDeclaration.SETTER).firValueParameter
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
    override fun convertFunctionDeclaration(node: Node): FirStatement {
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
        node.getChildNodeByTokenId(KtTokens.IDENTIFIER_ID)?.let {
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
                    KtNodeTypes.MODIFIER_LIST_ID -> {
                        modifiers = convertModifierList(it)
                    }
                    KtNodeTypes.TYPE_PARAMETER_LIST_ID -> typeParameterList = it
                    KtNodeTypes.VALUE_PARAMETER_LIST_ID -> valueParametersList = it //must convert later, because it can contain "return"
                    KtTokens.COLON_ID -> isReturnType = true
                    KtNodeTypes.TYPE_REFERENCE_ID -> if (isReturnType) returnType = convertType(it) else receiverTypeNode = it
                    KtNodeTypes.TYPE_CONSTRAINT_LIST_ID -> typeConstraints += convertTypeConstraints(it)
                    KtNodeTypes.CONTRACT_EFFECT_LIST_ID -> outerContractDescription = obtainContractDescription(it)
                    KtNodeTypes.BLOCK_ID -> block = it
                    KtTokens.EQ_ID -> hasEqToken = true
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
                            if (isAnonymousFunction) NodeTypeAnalyzer.ValueParameterDeclaration.LAMBDA else NodeTypeAnalyzer.ValueParameterDeclaration.FUNCTION
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
    override fun convertBlock(
        node: Node?,
        convertOnlyFirstStatement: Boolean,
    ): FirBlock {
        if (node == null) return buildEmptyExpressionBlock()
        if (node.toTokenId() != KtNodeTypes.BLOCK_ID) {
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
                KtNodeTypes.SUPER_TYPE_ENTRY_ID -> {
                    superTypeRefs += convertType(it)
                    index++
                }
                KtNodeTypes.SUPER_TYPE_CALL_ENTRY_ID -> convertConstructorInvocation(it).apply {
                    superTypeCalls += DelegatedConstructorWrapper(first, second, it.toFirSourceElement() as KtLightSourceElement)
                    superTypeRefs += first
                    index++
                }
                KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY_ID -> {
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
                KtNodeTypes.CONSTRUCTOR_CALLEE_ID -> firTypeRef = convertType(it)
                KtNodeTypes.VALUE_ARGUMENT_LIST_ID -> firValueArguments += expressionConverter.convertValueArguments(it)
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
                KtNodeTypes.TYPE_REFERENCE_ID -> firTypeRef = convertType(it)
                else -> if (it.isExpression()) expressionNode = it
            }
        }

        delegateFieldsMap[index] = buildField {
            source = explicitDelegation.toFirSourceElement().fakeElement(KtFakeSourceElementKind.ClassDelegationField)
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Synthetic.DelegateField
            name = NameUtils.delegateFieldName(delegateFieldsMap.size)
            symbol = FirFieldSymbol(CallableId(context.currentClassId, name))
            returnTypeRef = firTypeRef
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
                KtNodeTypes.TYPE_PARAMETER_ID -> container += convertTypeParameter(node, typeConstraints, containingDeclarationSymbol)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeConstraintList
     */
    private fun convertTypeConstraints(typeConstraints: Node): List<TypeConstraint> {
        return typeConstraints.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypes.TYPE_CONSTRAINT_ID -> container += convertTypeConstraint(node)
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
                KtNodeTypes.ANNOTATION_ENTRY_ID -> {
                    annotations +=
                        convertAnnotationEntry(
                            it,
                            diagnostic = ConeSimpleDiagnostic(
                                "Type parameter annotations are not allowed inside where clauses", DiagnosticKind.AnnotationInWhereClause,
                            )
                        )
                }
                KtNodeTypes.REFERENCE_EXPRESSION_ID -> {
                    identifier = it.asText
                    referenceExpression = it
                }
                KtNodeTypes.TYPE_REFERENCE_ID -> firType = convertType(it)
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
                KtNodeTypes.MODIFIER_LIST_ID -> typeParameterModifiers = convertTypeParameterModifiers(it)
                KtTokens.IDENTIFIER_ID -> identifier = it.asText
                KtNodeTypes.TYPE_REFERENCE_ID -> firType = convertType(it)
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
    override fun convertType(type: Node): FirTypeRef {
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
                KtNodeTypes.TYPE_REFERENCE_ID -> firType = convertType(it)
                KtNodeTypes.MODIFIER_LIST_ID -> allTypeModifiers += convertModifierList(it)
                KtNodeTypes.USER_TYPE_ID -> firType = convertUserType(typeRefSource, it)
                KtNodeTypes.NULLABLE_TYPE_ID -> firType = convertNullableType(typeRefSource, it, allTypeModifiers)
                KtNodeTypes.FUNCTION_TYPE_ID -> firType = convertFunctionType(typeRefSource, it, allTypeModifiers)
                KtNodeTypes.DYNAMIC_TYPE_ID -> firType = buildDynamicTypeRef {
                    source = typeRefSource
                    isMarkedNullable = false
                }
                KtNodeTypes.INTERSECTION_TYPE_ID -> firType = convertIntersectionType(typeRefSource, it, false)
                KtNodeTypes.CONTEXT_PARAMETER_LIST_ID, SyntaxElementTypesWithIds.NO_ID -> firType =
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
            if (it.toTokenId() != KtTokens.AND_ID) { //skip in forEachChildren?
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

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeRefContents
     */
    private fun convertReceiverType(receiverType: Node): FirTypeRef {
        var result: FirTypeRef? = null
        receiverType.forEachChildren {
            if (result != null) return@forEachChildren
            when (it.toTokenId()) {
                KtNodeTypes.TYPE_REFERENCE_ID -> result = convertType(it)
            }
        }

        //TODO specify error
        return result ?: throw Exception()
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
                KtNodeTypes.MODIFIER_LIST_ID -> allTypeModifiers += convertModifierList(it)
                KtNodeTypes.USER_TYPE_ID -> firType = convertUserType(typeRefSource, it, isNullable)
                KtNodeTypes.FUNCTION_TYPE_ID -> firType = convertFunctionType(typeRefSource, it, allTypeModifiers, isNullable)
                KtNodeTypes.NULLABLE_TYPE_ID -> firType = convertNullableType(typeRefSource, it, allTypeModifiers)
                KtNodeTypes.DYNAMIC_TYPE_ID -> firType = buildDynamicTypeRef {
                    source = typeRefSource
                    isMarkedNullable = true
                }
                KtNodeTypes.INTERSECTION_TYPE_ID -> firType = convertIntersectionType(typeRefSource, it, isNullable)
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
                KtNodeTypes.USER_TYPE_ID -> simpleFirUserType = convertUserType(typeRefSource, it) as? FirUserTypeRef //simple user type
                KtNodeTypes.REFERENCE_EXPRESSION_ID -> {
                    identifierSource = it.toFirSourceElement()
                    identifier = it.asText
                }
                KtNodeTypes.TYPE_ARGUMENT_LIST_ID -> {
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
    override fun convertTypeArguments(typeArguments: Node, allowedUnderscoredTypeArgument: Boolean): List<FirTypeProjection> {
        return typeArguments.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypes.TYPE_PROJECTION_ID -> container += convertTypeProjection(node, allowedUnderscoredTypeArgument)
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
                KtNodeTypes.MODIFIER_LIST_ID -> modifiers = convertTypeArgumentModifierList(it)
                KtNodeTypes.TYPE_REFERENCE_ID -> firType = convertType(it)
                KtTokens.MUL_ID -> isStarProjection = true
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
            val text = qualifierSource.toNode().getChildNodeByTokenId(KtTokens.IDENTIFIER_ID)?.asText
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
                KtNodeTypes.FUNCTION_TYPE_RECEIVER_ID -> receiverTypeReference = convertReceiverType(it)
                KtNodeTypes.VALUE_PARAMETER_LIST_ID -> parameters += convertFunctionTypeParameters(it)
                KtNodeTypes.TYPE_REFERENCE_ID -> returnTypeReference = convertType(it)
                KtNodeTypes.CONTEXT_PARAMETER_LIST_ID -> contextList = it
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
                    KtNodeTypes.CONTEXT_RECEIVER_ID, KtNodeTypes.VALUE_PARAMETER_ID -> {
                        val typeReference = it.getChildNodeByTokenId(KtNodeTypes.TYPE_REFERENCE_ID)

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
                KtNodeTypes.VALUE_PARAMETER_ID -> {
                    var name: Name? = null
                    var typeRef: FirTypeRef? = null
                    node.forEachChildren {
                        when (it.toTokenId()) {
                            KtTokens.IDENTIFIER_ID -> name = it.asText.nameAsSafeName()
                            KtNodeTypes.TYPE_REFERENCE_ID -> typeRef = convertType(it)
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
    override fun convertValueParameters(
        valueParameters: Node,
        functionSymbol: FirFunctionSymbol<*>,
        valueParameterDeclaration: NodeTypeAnalyzer.ValueParameterDeclaration,
        additionalAnnotations: List<FirAnnotation>
    ): List<ValueParameter<Node>> {
        return valueParameters.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                KtNodeTypes.VALUE_PARAMETER_ID -> container += convertValueParameter(
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
    override fun convertValueParameter(
        valueParameter: Node,
        containingDeclarationSymbol: FirBasedSymbol<*>?,
        valueParameterDeclaration: NodeTypeAnalyzer.ValueParameterDeclaration,
        additionalAnnotations: List<FirAnnotation>
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
                KtNodeTypes.MODIFIER_LIST_ID -> modifiers = convertModifierList(it)
                KtTokens.VAL_KEYWORD_ID -> isVal = true
                KtTokens.VAR_KEYWORD_ID -> isVar = true
                KtTokens.IDENTIFIER_ID -> identifier = it.asText
                KtNodeTypes.TYPE_REFERENCE_ID -> {}
                KtNodeTypes.DESTRUCTURING_DECLARATION_ID -> destructuringDeclaration = convertDestructingDeclaration(it)
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have default value")
            }
        }

        val name = convertValueParameterName(identifier.nameAsSafeName(), valueParameterDeclaration) { identifier }
        val valueParameterSymbol = FirValueParameterSymbol()
        context.withContainerSymbol(valueParameterSymbol, isLocal = !valueParameterDeclaration.isAnnotationOwner) {
            valueParameter.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypes.TYPE_REFERENCE_ID -> firType = convertType(it)
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
                isFromPrimaryConstructor = valueParameterDeclaration == NodeTypeAnalyzer.ValueParameterDeclaration.PRIMARY_CONSTRUCTOR,
                isContextParameter = valueParameterDeclaration == NodeTypeAnalyzer.ValueParameterDeclaration.CONTEXT_PARAMETER,
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
            contextList.getChildNodesByTokenId(KtNodeTypes.VALUE_PARAMETER_ID).mapTo(this) { contextParameterElement ->
                convertValueParameter(
                    valueParameter = contextParameterElement,
                    containingDeclarationSymbol = containingDeclarationSymbol,
                    valueParameterDeclaration = NodeTypeAnalyzer.ValueParameterDeclaration.CONTEXT_PARAMETER
                ).firValueParameter
            }

            // Legacy context receivers
            contextList.getChildNodesByTokenId(KtNodeTypes.CONTEXT_RECEIVER_ID).mapTo(this) { contextReceiverElement ->
                buildValueParameter {
                    this.source = contextReceiverElement.toFirSourceElement()
                    this.moduleData = baseModuleData
                    this.origin = FirDeclarationOrigin.Source

                    val customLabelName =
                        contextReceiverElement
                            .getChildNodeByTokenId(KtNodeTypes.LABEL_QUALIFIER_ID)
                            ?.getChildNodeByTokenId(KtNodeTypes.LABEL_ID)
                            ?.getChildNodeByTokenId(KtTokens.IDENTIFIER_ID)
                            ?.getReferencedNameAsName()

                    val typeReference = contextReceiverElement.getChildNodeByTokenId(KtNodeTypes.TYPE_REFERENCE_ID)

                    val labelNameFromTypeRef = typeReference?.getChildNodeByTokenId(KtNodeTypes.USER_TYPE_ID)
                        ?.getChildNodeByTokenId(KtNodeTypes.REFERENCE_EXPRESSION_ID)
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

    fun convertScript(
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

            val childNodes = script.getChildNodeByTokenId(KtNodeTypes.BLOCK_ID)?.getChildren().orEmpty()
                .filter { it.toTokenId() in scriptDeclarationTokensId }

            val scriptDeclarationsIter = childNodes.listIterator()
            context.withContainerScriptSymbol(symbol) {
                val modifierLists = mutableListOf<Node>()
                while (scriptDeclarationsIter.hasNext()) {
                    val declarationSource = scriptDeclarationsIter.next()
                    val isLast = !scriptDeclarationsIter.hasNext()
                    when (declarationSource.toTokenId()) {
                        KtNodeTypes.SCRIPT_INITIALIZER_ID -> {
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

                        KtNodeTypes.DESTRUCTURING_DECLARATION_ID -> {
                            val destructuringDeclaration = convertDestructingDeclaration(declarationSource)
                            val destructuringContainerVar = generateTemporaryVariable(
                                moduleData = baseModuleData,
                                source = declarationSource.toFirSourceElement(),
                                name = Name.special("<destruct>"),
                                initializer = destructuringDeclaration.initializer,
                                extractedAnnotations = destructuringDeclaration.annotations,
                                origin = FirDeclarationOrigin.Synthetic.ScriptTopLevelDestructuringDeclarationContainer
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
                                configureScriptDestructuringDeclarationEntry(it, destructuringContainerVar)
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

    val isDirectlyInsideCompanionBlock: Boolean
        get() = context.currentCompanionBlockOwnerOrNull.let { it != null && it == context.containerSymbolIfAny }

    private fun Node.isExpression(): Boolean = toTokenId().isExpression()

    private val scriptDeclarationTokensId = setOf(
        KtNodeTypes.CLASS_ID, KtNodeTypes.FUNCTION_ID, KtNodeTypes.PROPERTY_ID,
        KtNodeTypes.TYPEALIAS_ID, KtNodeTypes.OBJECT_DECLARATION_ID, KtNodeTypes.CLASS_INITIALIZER_ID,
        KtNodeTypes.MODIFIER_LIST_ID, KtNodeTypes.SCRIPT_INITIALIZER_ID, KtNodeTypes.DESTRUCTURING_DECLARATION_ID
    )
}
