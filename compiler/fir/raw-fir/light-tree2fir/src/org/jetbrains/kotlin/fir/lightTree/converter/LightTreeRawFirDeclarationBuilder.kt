/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.ElementTypeUtils.isExpression
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.firstFunctionCallInBlockHasLambdaArgumentWithLabel
import org.jetbrains.kotlin.fir.analysis.isCallTheFirstStatement
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.declarations.utils.DanglingTypeConstraint
import org.jetbrains.kotlin.fir.declarations.utils.addDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.utils.danglingTypeConstraints
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.lightTree.fir.*
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierList
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeParameterModifierList
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeProjectionModifierList
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.util.getChildren
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class LightTreeRawFirDeclarationBuilder(
    session: FirSession,
    internal val baseScopeProvider: FirScopeProvider,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    context: Context<LighterASTNode> = Context(),
) : AbstractLightTreeRawFirBuilder(session, tree, context) {

    private val expressionConverter = LightTreeRawFirExpressionBuilder(session, tree, this, context)

    /**
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parseFile]
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parsePreamble]
     */
    fun convertFile(file: LighterASTNode, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping): FirFile {
        if (file.tokenType != KT_FILE) {
            //TODO throw error
            throw Exception()
        }

        val fileSymbol = FirFileSymbol()
        val fileAnnotations = mutableListOf<FirAnnotation>()
        val importList = mutableListOf<FirImport>()
        val firDeclarationList = mutableListOf<FirDeclaration>()
        val modifierList = mutableListOf<LighterASTNode>()
        context.packageFqName = FqName.ROOT
        var packageDirective: FirPackageDirective? = null
        file.forEachChildren { child ->
            when (child.tokenType) {
                FILE_ANNOTATION_LIST -> {
                    withContainerSymbol(fileSymbol) {
                        convertAnnotationsOnlyTo(child, fileAnnotations)
                    }
                }
                PACKAGE_DIRECTIVE -> {
                    packageDirective = convertPackageDirective(child).also { context.packageFqName = it.packageFqName }
                }
                IMPORT_LIST -> importList += convertImportDirectives(child)
                CLASS -> firDeclarationList += convertClass(child)
                FUN -> firDeclarationList += convertFunctionDeclaration(child) as FirDeclaration
                KtNodeTypes.PROPERTY -> firDeclarationList += convertPropertyDeclaration(child)
                TYPEALIAS -> firDeclarationList += convertTypeAlias(child)
                OBJECT_DECLARATION -> firDeclarationList += convertClass(child)
                DESTRUCTURING_DECLARATION -> {
                    val initializer = buildFirDestructuringDeclarationInitializer(child)
                    firDeclarationList += buildErrorNonLocalDestructuringDeclaration(child.toFirSourceElement(), initializer)
                }
                SCRIPT -> {
                    // TODO: scripts aren't supported yet
                }
                MODIFIER_LIST -> modifierList += child
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
            declarations += firDeclarationList
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseBlockExpression
     */
    fun convertBlockExpression(block: LighterASTNode): FirBlock {
        return convertBlockExpressionWithoutBuilding(block).build()
    }

    fun convertBlockExpressionWithoutBuilding(block: LighterASTNode, kind: KtFakeSourceElementKind? = null): FirBlockBuilder {
        val firStatements = block.forEachChildrenReturnList<FirStatement> { node, container ->
            when (node.tokenType) {
                CLASS, OBJECT_DECLARATION -> container += convertClass(node) as FirStatement
                FUN -> container += convertFunctionDeclaration(node)
                KtNodeTypes.PROPERTY -> container += convertPropertyDeclaration(node) as FirStatement
                DESTRUCTURING_DECLARATION -> container +=
                    convertDestructingDeclaration(node).toFirDestructingDeclaration(this, baseModuleData)
                TYPEALIAS -> container += convertTypeAlias(node) as FirStatement
                CLASS_INITIALIZER -> shouldNotBeCalled("CLASS_INITIALIZER expected to be processed during class body conversion")
                else -> if (node.isExpression()) container += expressionConverter.getAsFirStatement(node)
            }
        }
        return FirBlockBuilder().apply {
            source = block.toFirSourceElement(kind)
            firStatements.forEach { firStatement ->
                val isForLoopBlock = firStatement is FirBlock && firStatement.source?.kind == KtFakeSourceElementKind.DesugaredForLoop
                if (firStatement !is FirBlock || isForLoopBlock || firStatement.annotations.isNotEmpty()) {
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
    private fun convertPackageDirective(packageNode: LighterASTNode): FirPackageDirective {
        var packageName: FqName = FqName.ROOT
        packageNode.forEachChildren {
            when (it.tokenType) {
                //TODO separate logic for both expression types
                DOT_QUALIFIED_EXPRESSION, REFERENCE_EXPRESSION -> packageName = parsePackageName(it)
            }
        }
        return buildPackageDirective {
            packageFqName = packageName
            source = packageNode.toFirSourceElement()
        }
    }

    private fun parsePackageName(node: LighterASTNode): FqName {
        var packageName: FqName = FqName.ROOT
        val parts = parsePackageParts(node)

        for (part in parts) {
            packageName = packageName.child(Name.identifier(part))
        }

        return packageName
    }

    private fun parsePackageParts(node: LighterASTNode): List<String> {
        fun parse(node: LighterASTNode): MutableList<String> {
            if (node.tokenType == DOT_QUALIFIED_EXPRESSION) {
                val children = node.getChildren(tree)

                if (children.size == 3) {
                    return parse(children.first()).apply {
                        add(children.last().getAsStringWithoutBacktick())
                    }
                }
            }

            if (node.tokenType == REFERENCE_EXPRESSION) {
                return mutableListOf(node.getAsStringWithoutBacktick())
            }

            return mutableListOf()
        }

        return parse(node)
    }

    private fun convertImportAlias(importAlias: LighterASTNode): Pair<String, KtSourceElement>? {
        importAlias.forEachChildren {
            when (it.tokenType) {
                IDENTIFIER -> return Pair(it.asText, it.toFirSourceElement())
            }
        }

        return null
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseImportDirective
     */
    private fun convertImportDirective(importDirective: LighterASTNode): FirImport {
        var importedFqName: FqName? = null
        var isAllUnder = false
        var aliasName: String? = null
        var aliasSource: KtSourceElement? = null
        importDirective.forEachChildren { child ->
            when (child.tokenType) {
                REFERENCE_EXPRESSION, DOT_QUALIFIED_EXPRESSION -> {
                    importedFqName = mutableListOf<String>()
                        .apply { collectSegments(child) }
                        .joinToString(".")
                        .let { FqName(it) }
                }
                MUL -> isAllUnder = true
                IMPORT_ALIAS -> {
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

    private fun MutableList<String>.collectSegments(expression: LighterASTNode) {
        when (expression.tokenType) {
            REFERENCE_EXPRESSION -> add(expression.getAsStringWithoutBacktick())
            DOT_QUALIFIED_EXPRESSION -> {
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
    private fun convertImportDirectives(importList: LighterASTNode): List<FirImport> {
        return importList.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                IMPORT_DIRECTIVE -> container += convertImportDirective(node)
            }
        }
    }

    /*****    MODIFIERS    *****/
    /**
     * Convert modifiers and collect annotations.
     *
     * To convert annotations, [ModifierList.convertAnnotationsTo] or [ModifierList.convertAnnotations] must be called inside
     * a [withContainerSymbol] block.
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertModifierList(modifiers: LighterASTNode, isInClass: Boolean = false): ModifierList {
        return ModifierList().also { it.consume(modifiers, isInClass) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentModifierList
     */
    private fun convertTypeArgumentModifierList(modifiers: LighterASTNode): TypeProjectionModifierList {
        return TypeProjectionModifierList().also { it.consume(modifiers) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentModifierList
     */
    private fun convertTypeParameterModifiers(modifiers: LighterASTNode): TypeParameterModifierList {
        return TypeParameterModifierList().also { it.consume(modifiers) }
    }

    private fun ModifierList.consume(modifierList: LighterASTNode, isInClass: Boolean = false) {
        modifierList.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> annotations += it
                ANNOTATION_ENTRY -> annotations += it
                CONTEXT_RECEIVER_LIST -> contextLists += it
                is KtModifierKeywordToken -> addModifier(it, isInClass)
            }
        }
    }

    /*****    ANNOTATIONS    *****/
    /**
     * Convert only annotations
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertAnnotationsOnlyTo(modifierList: LighterASTNode, list: MutableList<in FirAnnotationCall>) {
        modifierList.forEachChildren { node ->
            convertAnnotationOrAnnotationEntryTo(node, list)
        }
    }

    private fun ModifierList.convertAnnotationsTo(list: MutableList<in FirAnnotationCall>) {
        for (node in annotations) {
            convertAnnotationOrAnnotationEntryTo(node, list)
        }
    }

    private fun ModifierList.convertAnnotations(): List<FirAnnotationCall> {
        return buildList<FirAnnotationCall> { convertAnnotationsTo(this) }
    }

    private fun convertAnnotationOrAnnotationEntryTo(node: LighterASTNode, list: MutableList<in FirAnnotationCall>) {
        when (node.tokenType) {
            ANNOTATION -> convertAnnotationTo(node, list)
            ANNOTATION_ENTRY -> list += convertAnnotationEntry(node)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseAnnotationOrList
     */
    fun convertAnnotationTo(node: LighterASTNode, list: MutableList<in FirAnnotationCall>) {
        var annotationTarget: AnnotationUseSiteTarget? = null
        node.forEachChildren { child ->
            when (child.tokenType) {
                ANNOTATION_TARGET -> annotationTarget = convertAnnotationTarget(child)
                ANNOTATION_ENTRY -> list += convertAnnotationEntry(
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
    private fun convertAnnotationTarget(annotationUseSiteTarget: LighterASTNode): AnnotationUseSiteTarget {
        lateinit var annotationTarget: AnnotationUseSiteTarget
        annotationUseSiteTarget.forEachChildren {
            when (it.tokenType) {
                ALL_KEYWORD -> annotationTarget = ALL
                FIELD_KEYWORD -> annotationTarget = FIELD
                FILE_KEYWORD -> annotationTarget = FILE
                PROPERTY_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.PROPERTY
                GET_KEYWORD -> annotationTarget = PROPERTY_GETTER
                SET_KEYWORD -> annotationTarget = PROPERTY_SETTER
                RECEIVER_KEYWORD -> annotationTarget = RECEIVER
                PARAM_KEYWORD -> annotationTarget = CONSTRUCTOR_PARAMETER
                SETPARAM_KEYWORD -> annotationTarget = SETTER_PARAMETER
                DELEGATE_KEYWORD -> annotationTarget = PROPERTY_DELEGATE_FIELD
            }
        }

        return annotationTarget
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseAnnotation
     * can be treated as unescapedAnnotation
     */
    fun convertAnnotationEntry(
        unescapedAnnotation: LighterASTNode,
        defaultAnnotationUseSiteTarget: AnnotationUseSiteTarget? = null,
        diagnostic: ConeDiagnostic? = null,
    ): FirAnnotationCall = withForcedLocalContext {
        var annotationUseSiteTarget: AnnotationUseSiteTarget? = null
        lateinit var constructorCalleePair: Pair<FirTypeRef, List<FirExpression>>
        unescapedAnnotation.forEachChildren {
            when (it.tokenType) {
                ANNOTATION_TARGET -> annotationUseSiteTarget = convertAnnotationTarget(it)
                CONSTRUCTOR_CALLEE -> constructorCalleePair = convertConstructorInvocation(unescapedAnnotation)
            }
        }
        val qualifier = (constructorCalleePair.first as? FirUserTypeRef)?.qualifier?.last()
        val name = qualifier?.name ?: Name.special("<no-annotation-name>")
        val theCalleeReference = buildSimpleNamedReference {
            source = unescapedAnnotation
                .getChildNodeByType(CONSTRUCTOR_CALLEE)
                ?.getChildNodeByType(TYPE_REFERENCE)
                ?.getChildNodeByType(USER_TYPE)
                ?.getChildNodeByType(REFERENCE_EXPRESSION)
                ?.toFirSourceElement()
            this.name = name
        }

        if (diagnostic == null) {
            buildAnnotationCall {
                source = unescapedAnnotation.toFirSourceElement()
                useSiteTarget = annotationUseSiteTarget ?: defaultAnnotationUseSiteTarget
                annotationTypeRef = constructorCalleePair.first
                calleeReference = theCalleeReference
                extractArgumentsFrom(constructorCalleePair.second)
                typeArguments += qualifier?.typeArgumentList?.typeArguments ?: listOf()
                containingDeclarationSymbol = context.containerSymbol
            }
        } else {
            buildErrorAnnotationCall {
                source = unescapedAnnotation.toFirSourceElement()
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

    private fun LighterASTNode.hasValueParameters(): Boolean {
        return getChildNodesByType(VALUE_PARAMETER_LIST).let {
            it.isNotEmpty() && it.first().getChildNodesByType(VALUE_PARAMETER).isNotEmpty()
        }
    }

    /*****    DECLARATIONS    *****/
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassOrObject
     */
    private fun convertClass(classNode: LighterASTNode): FirDeclaration {
        var modifiers: ModifierList? = null
        var classKind: ClassKind = ClassKind.CLASS
        var identifier: String? = null
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        var primaryConstructor: LighterASTNode? = null
        val typeConstraints = mutableListOf<TypeConstraint>()
        var classBody: LighterASTNode? = null
        var superTypeList: LighterASTNode? = null
        var typeParameterList: LighterASTNode? = null
        classNode.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it, isInClass = true)
                IDENTIFIER -> identifier = it.asText
            }
        }

        val calculatedModifiers = modifiers ?: ModifierList()
        val className = identifier.nameAsSafeName(if (calculatedModifiers.isCompanion()) "Companion" else "")
        val isLocalWithinParent = classNode.getParent()?.elementType != CLASS_BODY && isClassLocal(classNode) { getParent() }
        val classIsExpect = calculatedModifiers.hasExpect() || context.containerIsExpect

        return withChildClassName(className, isExpect = classIsExpect, isLocalWithinParent) {
            val classSymbol = FirRegularClassSymbol(context.currentClassId)
            withContainerSymbol(classSymbol) {
                classNode.forEachChildren {
                    when (it.tokenType) {
                        CLASS_KEYWORD -> classKind = ClassKind.CLASS
                        INTERFACE_KEYWORD -> classKind = ClassKind.INTERFACE
                        OBJECT_KEYWORD -> classKind = ClassKind.OBJECT
                        TYPE_PARAMETER_LIST -> typeParameterList = it
                        PRIMARY_CONSTRUCTOR -> primaryConstructor = it
                        SUPER_TYPE_LIST -> superTypeList = it
                        TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                        CLASS_BODY -> classBody = it
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

                withCapturedTypeParameters(
                    // Transferring phantom type parameters to objects is cursed as they are
                    // accessible by qualifier `MyObject`, which is an expression and must have
                    // some single type.
                    // Letting their types contain no type arguments while the class itself
                    // expects some sounds fragile.
                    status = status.isInner || isLocal && !classKind.isObject,
                    declarationSource = classNode.toFirSourceElement(),
                    currentFirTypeParameters = firTypeParameters,
                ) {
                    var delegatedFieldsMap: Map<Int, FirFieldSymbol>? = null
                    buildRegularClass {
                        source = classNode.toFirSourceElement()
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

                        val selfType = classNode.toDelegatedSelfType(this)
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
                                    source = classNode.toFirSourceElement(KtFakeSourceElementKind.EnumSuperTypeRef)
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

                        val secondaryConstructors = classBody.getChildNodesByType(SECONDARY_CONSTRUCTOR)
                        val classWrapper = ClassWrapper(
                            calculatedModifiers, classKind, this, hasSecondaryConstructor = secondaryConstructors.isNotEmpty(),
                            hasDefaultConstructor = if (primaryConstructor != null) !primaryConstructor.hasValueParameters()
                            else secondaryConstructors.isEmpty() || secondaryConstructors.any { !it.hasValueParameters() },
                            delegatedSelfTypeRef = selfType,
                            delegatedSuperTypeRef = delegatedSuperTypeRef ?: FirImplicitTypeRefImplWithoutSource,
                            delegatedSuperCalls = delegationSpecifiers?.superTypeCalls ?: emptyList()
                        )
                        //parse primary constructor
                        val primaryConstructorWrapper = convertPrimaryConstructor(
                            primaryConstructor,
                            modifiers?.contextLists,
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
                                        currentDispatchReceiverType(),
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
                            val zippedParameters = properties.map { it.source!!.lighterASTNode to it }
                            DataClassMembersGenerator(
                                primaryConstructor ?: classNode,
                                this,
                                firPrimaryConstructor,
                                zippedParameters,
                                context.packageFqName,
                                context.className,
                                addValueParameterAnnotations = { valueParam ->
                                    withContainerSymbol(symbol) {
                                        valueParam.forEachChildren { node ->
                                            if (node.tokenType == MODIFIER_LIST) {
                                                buildList {
                                                    convertAnnotationsOnlyTo(node, this)
                                                }.filterTo(annotations) {
                                                    it.useSiteTarget.appliesToPrimaryConstructorParameter()
                                                }
                                            }
                                        }
                                    }
                                },
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
                    }.also {
                        it.delegateFieldsMap = delegatedFieldsMap
                    }
                }.also {
                    fillDanglingConstraintsTo(firTypeParameters, typeConstraints, it)
                }
            }
        }.also {
            if (classNode.getParent()?.elementType == KtStubElementTypes.CLASS_BODY) {
                it.initContainingClassForLocalAttr()
            }
        }
    }

    /**
     * see PsiRawFirBuilder.Visitor.visitObjectLiteralExpression
     *
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseObjectLiteral
     */
    fun convertObjectLiteral(objectLiteral: LighterASTNode): FirElement {
        return withChildClassName(SpecialNames.ANONYMOUS, forceLocalContext = true, isExpect = false) {
            var delegatedFieldsMap: Map<Int, FirFieldSymbol>? = null
            buildAnonymousObjectExpression {
                source = objectLiteral.toFirSourceElement()
                anonymousObject = buildAnonymousObject {
                    val objectDeclaration = objectLiteral.getChildNodesByType(OBJECT_DECLARATION).first()
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

                    var modifiers: ModifierList? = null
                    var primaryConstructor: LighterASTNode? = null
                    val superTypeRefs = mutableListOf<FirTypeRef>()
                    var delegatedSuperTypeRef: FirTypeRef? = null
                    var classBody: LighterASTNode? = null
                    var delegatedConstructorSource: KtLightSourceElement? = null
                    var delegatedSuperCalls: List<DelegatedConstructorWrapper>? = null
                    var delegateFields: List<FirField>? = null

                    objectDeclaration.forEachChildren { child ->
                        when (child.tokenType) {
                            MODIFIER_LIST -> {
                                modifiers = convertModifierList(child)
                            }
                            PRIMARY_CONSTRUCTOR -> primaryConstructor = child
                            SUPER_TYPE_LIST -> convertDelegationSpecifiers(child).let { specifiers ->
                                delegatedSuperTypeRef = specifiers.superTypeCalls.lastOrNull()?.delegatedSuperTypeRef
                                superTypeRefs += specifiers.superTypesRef
                                delegatedConstructorSource = specifiers.superTypeCalls.lastOrNull()?.source
                                delegateFields = specifiers.delegateFieldsMap.values.map { it.fir }
                                delegatedFieldsMap = specifiers.delegateFieldsMap.takeIf { it.isNotEmpty() }
                                delegatedSuperCalls = specifiers.superTypeCalls
                            }
                            CLASS_BODY -> classBody = child
                        }
                    }

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
                        hasSecondaryConstructor = classBody.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                        hasDefaultConstructor = false,
                        delegatedSelfTypeRef = delegatedSelfType,
                        delegatedSuperTypeRef = delegatedSuperType,
                        delegatedSuperCalls = delegatedSuperCalls ?: emptyList(),
                    )
                    //parse primary constructor
                    convertPrimaryConstructor(
                        primaryConstructor,
                        modifiers?.contextLists,
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
                }.also {
                    it.delegateFieldsMap = delegatedFieldsMap
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumEntry
     */
    private fun convertEnumEntry(enumEntry: LighterASTNode, classWrapper: ClassWrapper): FirEnumEntry {
        var modifiers: ModifierList? = null
        lateinit var identifier: String
        val enumSuperTypeCallEntry = mutableListOf<FirExpression>()
        var classBodyNode: LighterASTNode? = null
        var superTypeCallEntry: LighterASTNode? = null
        enumEntry.getChildNodeByType(IDENTIFIER)?.let {
            identifier = it.asText
        }

        val enumEntryName = identifier.nameAsSafeName()
        val containingClassIsExpectClass = classWrapper.hasExpect() || context.containerIsExpect
        return buildEnumEntry {
            symbol = FirEnumEntrySymbol(CallableId(context.currentClassId, enumEntryName))
            withContainerSymbol(symbol) {
                enumEntry.forEachChildren {
                    when (it.tokenType) {
                        MODIFIER_LIST -> {
                            modifiers = convertModifierList(it)
                        }
                        INITIALIZER_LIST -> {
                            enumSuperTypeCallEntry += convertInitializerList(it)
                            it.getChildNodeByType(SUPER_TYPE_CALL_ENTRY)?.let { superTypeCall ->
                                superTypeCallEntry = superTypeCall
                            }
                        }
                        CLASS_BODY -> classBodyNode = it
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
                if (classWrapper.hasDefaultConstructor && enumEntry.getChildNodeByType(INITIALIZER_LIST) == null &&
                    modifiers.let { it == null || it.annotations.isEmpty() } && classBodyNode == null
                ) {
                    return@buildEnumEntry
                }
                modifiers?.convertAnnotationsTo(annotations)
                initializer = withChildClassName(enumEntryName, isExpect = false) {
                    buildAnonymousObjectExpression {
                        val entrySource = enumEntry.toFirSourceElement(KtFakeSourceElementKind.EnumInitializer)
                        source = entrySource
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
                                hasSecondaryConstructor = classBodyNode.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
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
                                        superTypeCallEntry?.toFirSourceElement(),
                                    )
                                )
                            )
                            superTypeRefs += enumClassWrapper.delegatedSuperTypeRef
                            convertPrimaryConstructor(
                                null,
                                modifiers?.contextLists,
                                enumEntry.toFirSourceElement(),
                                enumClassWrapper,
                                superTypeCallEntry?.toFirSourceElement(),
                                isEnumEntry = true,
                                containingClassIsExpectClass = containingClassIsExpectClass
                            )?.let { declarations += it.firConstructor }
                            classBodyNode?.also {
                                // Use ANONYMOUS_OBJECT_NAME for the owner class id of enum entry declarations
                                withChildClassName(SpecialNames.ANONYMOUS, forceLocalContext = true, isExpect = false) {
                                    declarations += convertClassBody(it, enumClassWrapper)
                                }
                            }
                        }
                    }
                }
            }
        }.also {
            it.containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumEntry
     */
    private fun convertInitializerList(initializerList: LighterASTNode): List<FirExpression> {
        val firValueArguments = mutableListOf<FirExpression>()
        initializerList.forEachChildren {
            when (it.tokenType) {
                SUPER_TYPE_CALL_ENTRY -> convertConstructorInvocation(it).apply {
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
    private fun convertClassBody(classBody: LighterASTNode, classWrapper: ClassWrapper): List<FirDeclaration> {
        val modifierLists = mutableListOf<LighterASTNode>()
        var firDeclarations = classBody.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                ENUM_ENTRY -> container += convertEnumEntry(node, classWrapper)
                CLASS -> container += convertClass(node)
                FUN -> container += convertFunctionDeclaration(node) as FirDeclaration
                KtNodeTypes.PROPERTY -> container += convertPropertyDeclaration(node, classWrapper)
                TYPEALIAS -> container += convertTypeAlias(node)
                OBJECT_DECLARATION -> container += convertClass(node)
                CLASS_INITIALIZER -> container += convertAnonymousInitializer(node, classWrapper) //anonymousInitializer
                SECONDARY_CONSTRUCTOR -> container += convertSecondaryConstructor(node, classWrapper)
                MODIFIER_LIST -> modifierLists += node
                DESTRUCTURING_DECLARATION -> {
                    val initializer = buildFirDestructuringDeclarationInitializer(node)
                    container += buildErrorNonLocalDestructuringDeclaration(node.toFirSourceElement(), initializer)
                }
            }
        }

        for (node in modifierLists) {
            firDeclarations += buildErrorNonLocalDeclarationForDanglingModifierList(node).apply {
                containingClassAttr = currentDispatchReceiverType()?.lookupTag
            }
        }
        return firDeclarations
    }

    private fun buildFirDestructuringDeclarationInitializer(destructuringDeclaration: LighterASTNode): FirExpression {
        val initializer = destructuringDeclaration.getChildExpression().takeUnless { it?.tokenType == PROPERTY_DELEGATE }
        return expressionConverter.getAsFirExpression(
            initializer,
            "Initializer required for destructuring declaration",
            sourceWhenInvalidExpression = destructuringDeclaration
        )
    }

    private fun buildErrorNonLocalDeclarationForDanglingModifierList(node: LighterASTNode) = buildDanglingModifierList {
        this.source = node.toFirSourceElement(KtFakeSourceElementKind.DanglingModifierList)
        moduleData = baseModuleData
        origin = FirDeclarationOrigin.Source
        diagnostic = ConeDanglingModifierOnTopLevel
        symbol = FirDanglingModifierSymbol()
        withContainerSymbol(symbol) {
            val modifiers = convertModifierList(node)
            contextParameters.addContextParameters(modifiers.contextLists, symbol)
            modifiers.convertAnnotationsTo(annotations)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassOrObject
     * primaryConstructor branch
     */
    private fun convertPrimaryConstructor(
        primaryConstructor: LighterASTNode?,
        classContextReceiverLists: List<LighterASTNode>?,
        selfTypeSource: KtSourceElement?,
        classWrapper: ClassWrapper,
        delegatedConstructorSource: KtLightSourceElement?,
        isEnumEntry: Boolean = false,
        containingClassIsExpectClass: Boolean,
        isImplicitlyActual: Boolean = false,
        isKotlinAny: Boolean = false,
    ): PrimaryConstructor? {
        val shouldGenerateImplicitConstructor =
            (classWrapper.isEnumEntry() || !classWrapper.hasSecondaryConstructor) &&
                    !classWrapper.isInterface() &&
                    (!containingClassIsExpectClass || classWrapper.classBuilder.classKind == ClassKind.ENUM_ENTRY)
        val isErrorConstructor = primaryConstructor == null && !shouldGenerateImplicitConstructor
        if (isErrorConstructor && classWrapper.delegatedSuperCalls.isEmpty()) {
            return null
        }

        val constructorSymbol = FirConstructorSymbol(callableIdForClassConstructor())
        withContainerSymbol(constructorSymbol) {
            var modifiersIfPresent: ModifierList? = null
            val valueParameters = mutableListOf<ValueParameter>()
            var hasConstructorKeyword = false
            primaryConstructor?.forEachChildren {
                when (it.tokenType) {
                    MODIFIER_LIST -> {
                        modifiersIfPresent = convertModifierList(it)
                    }
                    CONSTRUCTOR_KEYWORD -> hasConstructorKeyword = true
                    VALUE_PARAMETER_LIST -> valueParameters += convertValueParameters(
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

            val firDelegatedCall = runIf(generateDelegatedSuperCall) {
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
                            //[dirty] in case of enum classWrapper.delegatedSuperTypeRef.source is whole enum source
                            source = if (!isEnumEntry) {
                                classWrapper.delegatedSuperTypeRef.source?.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                                    ?: this@buildDelegatedConstructorCall.source?.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                            } else {
                                delegatedConstructorSource
                                    ?.lighterASTNode
                                    ?.getChildNodeByType(CONSTRUCTOR_CALLEE)
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
                symbol = constructorSymbol
                modifiersIfPresent?.convertAnnotationsTo(annotations)
                typeParameters += constructorTypeParametersFromConstructedClass(classWrapper.classBuilder.typeParameters)
                this.valueParameters += valueParameters.map { it.firValueParameter }
                delegatedConstructor = firDelegatedCall
                this.body = null
                this.contextParameters.addContextParameters(classContextReceiverLists, constructorSymbol)
            }

            return PrimaryConstructor(
                builder.build().apply {
                    containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
                },
                valueParameters,
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseMemberDeclarationRest
     * at INIT keyword
     */
    private fun convertAnonymousInitializer(
        anonymousInitializer: LighterASTNode,
        classWrapper: ClassWrapper
    ): FirDeclaration {
        val initializerSymbol = FirAnonymousInitializerSymbol()
        withContainerSymbol(initializerSymbol) {
            return buildAnonymousInitializer {
                var firBlock: FirBlock? = null

                anonymousInitializer.forEachChildren {
                    when (it.tokenType) {
                        MODIFIER_LIST -> convertAnnotationsOnlyTo(it, annotations)
                        BLOCK -> withForcedLocalContext {
                            firBlock = convertBlock(it)
                        }
                    }
                }

                symbol = initializerSymbol
                source = anonymousInitializer.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                body = firBlock ?: buildEmptyExpressionBlock()
                containingDeclarationSymbol = classWrapper.classBuilder.ownerRegularOrAnonymousObjectSymbol
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseSecondaryConstructor
     */
    private fun convertSecondaryConstructor(secondaryConstructor: LighterASTNode, classWrapper: ClassWrapper): FirConstructor {
        var modifiers: ModifierList? = null
        val firValueParameters = mutableListOf<ValueParameter>()
        var constructorDelegationCall: FirDelegatedConstructorCall? = null
        var block: LighterASTNode? = null

        val constructorSymbol = FirConstructorSymbol(callableIdForClassConstructor())
        withContainerSymbol(constructorSymbol) {
            var delegatedConstructorNode: LighterASTNode? = null
            secondaryConstructor.forEachChildren {
                when (it.tokenType) {
                    MODIFIER_LIST -> {
                        modifiers = convertModifierList(it)
                    }
                    VALUE_PARAMETER_LIST -> firValueParameters += convertValueParameters(
                        it,
                        constructorSymbol,
                        ValueParameterDeclaration.FUNCTION
                    )
                    CONSTRUCTOR_DELEGATION_CALL -> delegatedConstructorNode = it
                    BLOCK -> block = it
                }
            }

            val delegatedSelfTypeRef = classWrapper.delegatedSelfTypeRef
            val calculatedModifiers = modifiers ?: ModifierList()
            val isExpect = calculatedModifiers.hasExpect() || context.containerIsExpect
            if (delegatedConstructorNode != null) {
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
            return buildConstructor {
                source = secondaryConstructor.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = delegatedSelfTypeRef
                dispatchReceiverType = classWrapper.obtainDispatchReceiverForConstructor()
                this.status = status
                symbol = constructorSymbol
                delegatedConstructor = constructorDelegationCall

                context.firFunctionTargets += target
                modifiers?.convertAnnotationsTo(annotations)
                typeParameters += constructorTypeParametersFromConstructedClass(classWrapper.classBuilder.typeParameters)
                valueParameters += firValueParameters.map { it.firValueParameter }
                val (body, contractDescription) = withForcedLocalContext {
                    convertFunctionBody(block, null, allowLegacyContractDescription = true)
                }
                this.body = body
                contractDescription?.let { this.contractDescription = it }
                context.firFunctionTargets.removeLast()
                this.contextParameters.addContextParameters(classWrapper.modifiers.contextLists, constructorSymbol)
                this.contextParameters.addContextParameters(modifiers?.contextLists, constructorSymbol)
            }.also {
                it.containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
                target.bind(it)
            }
        }
    }

    private fun ClassWrapper.obtainDispatchReceiverForConstructor(): ConeClassLikeType? =
        if (isInner()) dispatchReceiverForInnerClassConstructor() else null

    /**
     * see PsiRawFirBuilder.Visitor.convert(KtConstructorDelegationCall, FirTypeRef, Boolean)
     */
    private fun convertConstructorDelegationCall(
        constructorDelegationCall: LighterASTNode,
        classWrapper: ClassWrapper,
        isExpect: Boolean,
    ): FirDelegatedConstructorCall? {
        var thisKeywordPresent = false
        val firValueArguments = mutableListOf<FirExpression>()
        constructorDelegationCall.forEachChildren {
            when (it.tokenType) {
                CONSTRUCTOR_DELEGATION_REFERENCE -> if (it.asText == "this") thisKeywordPresent = true
                VALUE_ARGUMENT_LIST -> firValueArguments += expressionConverter.convertValueArguments(it)
            }
        }

        val isImplicit = constructorDelegationCall.textLength == 0
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
            val calleeSource = constructorDelegationCall.getChildNodeByType(CONSTRUCTOR_DELEGATION_REFERENCE)
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
    private fun convertTypeAlias(typeAlias: LighterASTNode): FirDeclaration {
        var modifiers: ModifierList? = null
        var identifier: String? = null
        lateinit var typeRefNode: LighterASTNode
        var typeParametersNode: LighterASTNode? = null

        typeAlias.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_REFERENCE -> typeRefNode = it
                TYPE_PARAMETER_LIST -> typeParametersNode = it
            }
        }

        val calculatedModifiers = modifiers ?: ModifierList()
        val typeAliasName = identifier.nameAsSafeName()
        val typeAliasIsExpect = calculatedModifiers.hasExpect() || context.containerIsExpect
        return withChildClassName(typeAliasName, isExpect = typeAliasIsExpect) {
            val typeAliasSymbol = FirTypeAliasSymbol(context.currentClassId)
            withContainerSymbol(typeAliasSymbol) {
                val isInner = calculatedModifiers.isInner()
                buildTypeAlias {
                    source = typeAlias.toFirSourceElement()
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
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseProperty
     */
    fun convertPropertyDeclaration(property: LighterASTNode, classWrapper: ClassWrapper? = null): FirDeclaration {
        var modifiers: ModifierList? = null
        var identifier: String? = null
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        var isReturnType = false
        var delegate: LighterASTNode? = null
        var isVar = false
        var receiverTypeNode: LighterASTNode? = null
        var isStaticReceiver = false
        var staticReceiverTypeNode: LighterASTNode? = null
        var returnType: FirTypeRef = implicitType
        val typeConstraints = mutableListOf<TypeConstraint>()
        val accessors = mutableListOf<LighterASTNode>()
        var propertyInitializer: FirExpression? = null
        var typeParameterList: LighterASTNode? = null
        var fieldDeclaration: LighterASTNode? = null
        property.getChildNodeByType(IDENTIFIER)?.let {
            identifier = it.asText
        }

        val parentNode = property.getParent()
        val isLocal = !(parentNode?.tokenType == KT_FILE || parentNode?.tokenType == CLASS_BODY)
        val isInsideScript = context.containingScriptSymbol != null && context.className == FqName.ROOT
        val propertyName = when {
            (isLocal || isInsideScript) && identifier == "_" -> SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
            else -> identifier.nameAsSafeName()
        }
        val propertySymbol = if (isLocal) {
            FirPropertySymbol(propertyName)
        } else {
            FirPropertySymbol(callableIdForName(propertyName))
        }

        withContainerSymbol(propertySymbol, isLocal) {
            val propertySource = property.toFirSourceElement()
            property.forEachChildren {
                when (it.tokenType) {
                    MODIFIER_LIST -> {
                        modifiers = convertModifierList(it)
                    }
                    TYPE_PARAMETER_LIST -> typeParameterList = it
                    COLON -> isReturnType = true
                    TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverTypeNode = it
                    TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                    PROPERTY_DELEGATE -> delegate = it
                    VAR_KEYWORD -> isVar = true
                    PROPERTY_ACCESSOR -> {
                        accessors += it
                    }
                    BACKING_FIELD -> fieldDeclaration = it
                    COLONCOLON -> isStaticReceiver = true
                    USER_TYPE -> if (isStaticReceiver) staticReceiverTypeNode = it
                    else -> if (it.isExpression()) {
                        context.calleeNamesForLambda += null
                        propertyInitializer = withForcedLocalContext {
                            expressionConverter.getAsFirExpression(it, "Should have initializer")
                        }
                        context.calleeNamesForLambda.removeLast()
                    }
                }
            }

            val calculatedModifiers = modifiers ?: ModifierList()
            val propertyAnnotations = calculatedModifiers.convertAnnotations()

            return buildProperty {
                source = propertySource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = returnType
                staticReceiverParameter = staticReceiverTypeNode?.let { convertUserType(it.toFirSourceElement(), it) }
                name = propertyName
                this.isVar = isVar

                receiverParameter = receiverTypeNode?.let { createReceiverParameter({ convertType(it) }, moduleData, propertySymbol) }
                initializer = propertyInitializer

                //probably can do this for delegateExpression itself
                val delegateSource = delegate?.let {
                    (it.getChildExpression() ?: it).toFirSourceElement()
                }

                symbol = propertySymbol

                typeParameterList?.let { firTypeParameters += convertTypeParameters(it, typeConstraints, symbol) }

                backingField = fieldDeclaration.convertBackingField(
                    symbol, calculatedModifiers, returnType, isVar,
                    if (isLocal) emptyList() else propertyAnnotations.filter {
                        it.useSiteTarget == FIELD || it.useSiteTarget == PROPERTY_DELEGATE_FIELD
                    },
                    property,
                )

                if (isLocal) {
                    this.isLocal = true
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
                    this.isLocal = false

                    dispatchReceiverType = currentDispatchReceiverType()
                    withCapturedTypeParameters(true, propertySource, firTypeParameters) {
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
                            }

                        val convertedAccessors = accessors.map {
                            convertGetterOrSetter(it, returnType, propertyVisibility, symbol, calculatedModifiers, propertyAnnotations)
                        }
                        this.getter = convertedAccessors.find { it.isGetter }
                            ?: FirDefaultPropertyGetter(
                                source = property.toFirSourceElement(KtFakeSourceElementKind.DefaultAccessor),
                                moduleData = moduleData,
                                origin = FirDeclarationOrigin.Source,
                                propertyTypeRef = returnType.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                                visibility = propertyVisibility,
                                propertySymbol = symbol,
                                modality = calculatedModifiers.getModality(isClassOrObject = false),
                            ).also {
                                it.status = defaultAccessorStatus()
                                it.replaceAnnotations(propertyAnnotations.filterUseSiteTarget(PROPERTY_GETTER))
                                it.initContainingClassAttr()
                            }
                        // NOTE: We still need the setter even for a val property so we can report errors (e.g., VAL_WITH_SETTER).
                        this.setter = convertedAccessors.find { it.isSetter }
                            ?: if (isVar) {
                                FirDefaultPropertySetter(
                                    source = property.toFirSourceElement(KtFakeSourceElementKind.DefaultAccessor),
                                    moduleData = moduleData,
                                    origin = FirDeclarationOrigin.Source,
                                    propertyTypeRef = returnType.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
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
                            isStatic = calculatedModifiers.hasStatic()
                        }

                        generateAccessorsByDelegate(
                            delegateBuilder,
                            baseModuleData,
                            classWrapper?.classBuilder?.ownerRegularOrAnonymousObjectSymbol,
                            context,
                            isExtension = receiverTypeNode != null,
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
                }
            }
        }
    }

    /**
     * see PsiRawFirBuilder.Visitor.visitDestructuringDeclaration
     */
    internal fun convertDestructingDeclaration(destructingDeclaration: LighterASTNode): DestructuringDeclaration {
        val annotations = mutableListOf<FirAnnotationCall>()
        var isVar = false
        val entries = mutableListOf<DestructuringEntry>()
        val source = destructingDeclaration.toFirSourceElement()
        var firExpression: FirExpression? = null
        destructingDeclaration.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> convertAnnotationsOnlyTo(it, annotations)
                VAR_KEYWORD -> isVar = true
                DESTRUCTURING_DECLARATION_ENTRY -> entries += convertDestructingDeclarationEntry(it)
                // Property delegates should be ignored as they aren't a valid initializers
                PROPERTY_DELEGATE -> {}
                else -> if (it.isExpression()) firExpression =
                    expressionConverter.getAsFirExpression(it, "Initializer required for destructuring declaration")
            }
        }

        return DestructuringDeclaration(
            isVar,
            entries,
            firExpression ?: buildErrorExpression(
                destructingDeclaration.toFirSourceElement(),
                ConeSyntaxDiagnostic("Initializer required for destructuring declaration")
            ),
            source,
            annotations
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseMultiDeclarationName
     */
    private fun convertDestructingDeclarationEntry(entry: LighterASTNode): DestructuringEntry {
        val annotations = mutableListOf<FirAnnotationCall>()
        var identifier: String? = null
        var firType: FirTypeRef? = null
        entry.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> convertAnnotationsOnlyTo(it, annotations)
                IDENTIFIER -> identifier = it.asText
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val name = if (identifier == "_") {
            SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
        } else {
            identifier.nameAsSafeName()
        }

        return DestructuringEntry(
            source = entry.toFirSourceElement(),
            returnTypeRef = firType ?: implicitType,
            name = name,
            annotations = annotations,
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyComponent
     */
    private fun convertGetterOrSetter(
        getterOrSetter: LighterASTNode,
        propertyTypeRef: FirTypeRef,
        propertyVisibility: Visibility,
        propertySymbol: FirPropertySymbol,
        propertyModifiers: ModifierList,
        propertyAnnotations: List<FirAnnotationCall>,
    ): FirPropertyAccessor {
        var modifiers: ModifierList? = null
        var isGetter = true
        var returnType: FirTypeRef? = null
        val propertyTypeRefToUse = propertyTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
        val sourceElement = getterOrSetter.toFirSourceElement()
        val accessorSymbol = FirPropertyAccessorSymbol()
        var firValueParameters: FirValueParameter = buildDefaultSetterValueParameter {
            moduleData = baseModuleData
            containingDeclarationSymbol = accessorSymbol
            origin = FirDeclarationOrigin.Source
            source = sourceElement.fakeElement(KtFakeSourceElementKind.DefaultAccessor)
            returnTypeRef = propertyTypeRefToUse
            symbol = FirValueParameterSymbol(StandardNames.DEFAULT_VALUE_PARAMETER)
        }
        var block: LighterASTNode? = null
        var expression: LighterASTNode? = null
        var outerContractDescription: FirContractDescription? = null


        getterOrSetter.forEachChildren {
            if (it.asText == "set") isGetter = false
            when (it.tokenType) {
                SET_KEYWORD -> isGetter = false
                MODIFIER_LIST -> {
                    modifiers = convertModifierList(it)
                }
                TYPE_REFERENCE -> returnType = convertType(it)
                VALUE_PARAMETER_LIST -> {
                    // getter can have an empty value parameter list
                    if (!isGetter) {
                        firValueParameters = convertSetterParameter(
                            it, accessorSymbol, propertyTypeRefToUse, propertyAnnotations.filterUseSiteTarget(SETTER_PARAMETER)
                        )
                    }
                }
                CONTRACT_EFFECT_LIST -> outerContractDescription = obtainContractDescription(it)
                BLOCK -> block = it
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
            val bodyWithContractDescription = withForcedLocalContext {
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
    private fun LighterASTNode?.convertBackingField(
        propertySymbol: FirPropertySymbol,
        propertyModifiers: ModifierList,
        propertyReturnType: FirTypeRef,
        isVar: Boolean,
        annotationsFromProperty: List<FirAnnotationCall>,
        property: LighterASTNode,
    ): FirBackingField {
        var modifiers: ModifierList? = null
        var returnType: FirTypeRef = implicitType
        var backingFieldInitializer: FirExpression? = null
        this?.forEachChildren {
            when {
                it.tokenType == MODIFIER_LIST -> {
                    modifiers = convertModifierList(it)
                }
                it.tokenType == TYPE_REFERENCE -> returnType = convertType(it)
                it.isExpression() -> {
                    backingFieldInitializer = expressionConverter.getAsFirExpression(it, "Should have initializer")
                }
            }
        }
        val calculatedModifiers = modifiers ?: ModifierList()
        var componentVisibility = calculatedModifiers.getVisibility()
        if (componentVisibility == Visibilities.Unknown) {
            componentVisibility = Visibilities.Private
        }
        val status = obtainPropertyComponentStatus(componentVisibility, calculatedModifiers, propertyModifiers)
        val sourceElement = this?.toFirSourceElement()
        return if (this != null) {
            buildBackingField {
                source = sourceElement
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = returnType
                name = StandardNames.BACKING_FIELD
                symbol = FirBackingFieldSymbol(CallableId(name))
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
                source = property.toFirSourceElement(KtFakeSourceElementKind.DefaultAccessor),
                annotations = annotationsFromProperty.toMutableList(),
                returnTypeRef = propertyReturnType.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                isVar = isVar,
                propertySymbol = propertySymbol,
                status = status,
            )
        }
    }

    private fun obtainPropertyComponentStatus(
        componentVisibility: Visibility,
        modifiers: ModifierList,
        propertyModifiers: ModifierList,
    ): FirDeclarationStatus {
        // Downward propagation of `inline` and `external` modifiers (from property to its accessors)
        return FirDeclarationStatusImpl(componentVisibility, modifiers.getModality(isClassOrObject = false)).apply {
            isInline = propertyModifiers.hasInline() || modifiers.hasInline()
            isExternal = propertyModifiers.hasExternal() || modifiers.hasExternal()
            isLateInit = modifiers.hasLateinit()
        }
    }

    private fun obtainContractDescription(rawContractDescription: LighterASTNode): FirContractDescription =
        buildRawContractDescription {
            source = rawContractDescription.toFirSourceElement()
            extractRawEffects(rawContractDescription, rawEffects)
        }

    private fun extractRawEffects(rawContractDescription: LighterASTNode, destination: MutableList<FirExpression>) {
        rawContractDescription.forEachChildren {
            val errorReason = "The contract effect is not an expression"
            when (it.tokenType) {
                CONTRACT_EFFECT -> {
                    val effect = it.getFirstChild()
                    if (effect == null) {
                        val errorExpression =
                            buildErrorExpression(rawContractDescription.toFirSourceElement(), ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected))
                        destination.add(errorExpression)
                    } else {
                        val expression = expressionConverter.convertExpression(effect, errorReason)
                        destination.add(expression as FirExpression)
                    }
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
        setterParameter: LighterASTNode,
        functionSymbol: FirFunctionSymbol<*>,
        propertyTypeRef: FirTypeRef,
        additionalAnnotations: List<FirAnnotation>,
    ): FirValueParameter {
        var modifiers: ModifierList? = null
        lateinit var firValueParameter: FirValueParameter
        setterParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                VALUE_PARAMETER -> firValueParameter =
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
            symbol = FirValueParameterSymbol(firValueParameter.name)
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
    fun convertFunctionDeclaration(functionDeclaration: LighterASTNode): FirStatement {
        var modifiers: ModifierList? = null
        var identifier: String? = null
        var valueParametersList: LighterASTNode? = null
        var isReturnType = false
        var receiverTypeNode: LighterASTNode? = null
        var isStaticReceiver = false
        var staticReceiverTypeNode: LighterASTNode? = null
        var returnType: FirTypeRef? = null
        val typeConstraints = mutableListOf<TypeConstraint>()
        var block: LighterASTNode? = null
        var expression: LighterASTNode? = null
        var hasEqToken = false
        var typeParameterList: LighterASTNode? = null
        var outerContractDescription: FirContractDescription? = null
        functionDeclaration.getChildNodeByType(IDENTIFIER)?.let {
            identifier = it.asText
        }

        val parentNode = functionDeclaration.getParent()
        val isLocal = !(parentNode?.tokenType == KT_FILE || parentNode?.tokenType == CLASS_BODY)
        val functionSource = functionDeclaration.toFirSourceElement()
        val isAnonymousFunction = identifier == null && isLocal
        val functionName = identifier.nameAsSafeName()
        val functionSymbol: FirFunctionSymbol<*> = if (isAnonymousFunction) {
            FirAnonymousFunctionSymbol()
        } else {
            FirNamedFunctionSymbol(callableIdForName(functionName))
        }

        withContainerSymbol(functionSymbol, isLocal) {
            val target: FirFunctionTarget
            functionDeclaration.forEachChildren {
                when (it.tokenType) {
                    MODIFIER_LIST -> {
                        modifiers = convertModifierList(it)
                    }
                    TYPE_PARAMETER_LIST -> typeParameterList = it
                    VALUE_PARAMETER_LIST -> valueParametersList = it //must convert later, because it can contains "return"
                    COLON -> isReturnType = true
                    TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverTypeNode = it
                    TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                    CONTRACT_EFFECT_LIST -> outerContractDescription = obtainContractDescription(it)
                    BLOCK -> block = it
                    EQ -> hasEqToken = true
                    COLONCOLON -> isStaticReceiver = true
                    USER_TYPE -> if (isStaticReceiver) staticReceiverTypeNode = it
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
            val staticReceiver = staticReceiverTypeNode?.let { convertUserType(it.toFirSourceElement(), it) }
            val functionBuilder = if (isAnonymousFunction) {
                FirAnonymousFunctionBuilder().apply {
                    source = functionSource
                    receiverParameter = receiverTypeCalculator?.let { createReceiverParameter(it, baseModuleData, functionSymbol) }
                    staticReceiverParameter = staticReceiver
                    symbol = functionSymbol as FirAnonymousFunctionSymbol
                    isLambda = false
                    hasExplicitParameterList = true
                    label = context.getLastLabel(functionDeclaration)
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
                    val isStatic = calculatedModifiers.hasStatic()

                    if (isExpect || isActual || isOverride || isOperator || isInfix || isInline || isTailRec || isExternal || isSuspend || isStatic) {
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
                            isStatic = isStatic
                        )
                    }
                }
            } else {
                val labelName =
                    context.getLastLabel(functionDeclaration)?.name ?: runIf(!functionName.isSpecial) { functionName.identifier }
                target = FirFunctionTarget(labelName, isLambda = false)
                FirSimpleFunctionBuilder().apply {
                    source = functionSource
                    receiverParameter = receiverTypeCalculator?.let { createReceiverParameter(it, baseModuleData, functionSymbol) }
                    staticReceiverParameter = staticReceiver
                    name = functionName
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
                        isStatic = calculatedModifiers.hasStatic()
                    }

                    symbol = functionSymbol as FirNamedFunctionSymbol
                    dispatchReceiverType = runIf(!isLocal) { currentDispatchReceiverType() }
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

                withCapturedTypeParameters(true, functionSource, typeParameters) {
                    contextParameters.addContextParameters(modifiers?.contextLists, functionSymbol)

                    valueParametersList?.let { list ->
                        valueParameters += convertValueParameters(
                            list,
                            functionSymbol,
                            if (isAnonymousFunction) ValueParameterDeclaration.LAMBDA else ValueParameterDeclaration.FUNCTION
                        ).map { it.firValueParameter }
                    }

                    val allowLegacyContractDescription = outerContractDescription == null
                    val bodyWithContractDescription = withForcedLocalContext {
                        convertFunctionBody(block, expression, allowLegacyContractDescription)
                    }
                    this.body = bodyWithContractDescription.first
                    val contractDescription = outerContractDescription ?: bodyWithContractDescription.second
                    contractDescription?.let {
                        if (this is FirSimpleFunctionBuilder) {
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
        blockNode: LighterASTNode?,
        expression: LighterASTNode?,
        allowLegacyContractDescription: Boolean
    ): Pair<FirBlock?, FirContractDescription?> {
        return when {
            blockNode != null -> {
                val block = convertBlock(blockNode)
                val contractDescription = runIf(allowLegacyContractDescription) {
                    val blockSource = block.source
                    val diagnostic = when {
                        blockSource == null || !isCallTheFirstStatement(blockSource) -> ConeContractShouldBeFirstStatement
                        functionCallHasLabel(blockSource) -> ConeContractMayNotHaveLabel
                        else -> null
                    }
                    processLegacyContractDescription(block, diagnostic)
                }
                block to contractDescription
            }
            expression != null -> FirSingleExpressionBlock(
                expressionConverter.getAsFirExpression<FirExpression>(expression, "Function has no body (but should)").toReturn()
            ) to null
            else -> null to null
        }
    }

    private fun isCallTheFirstStatement(sourceElement: KtSourceElement): Boolean =
        isCallTheFirstStatement(sourceElement.lighterASTNode, { it.elementType }, { it.getChildren(sourceElement.treeStructure) })

    private fun functionCallHasLabel(sourceElement: KtSourceElement): Boolean {
        return firstFunctionCallInBlockHasLambdaArgumentWithLabel(
            sourceElement.lighterASTNode,
            { it.elementType },
            { it.getChildren(sourceElement.treeStructure) })
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseBlock
     */
    fun convertBlock(block: LighterASTNode?): FirBlock {
        if (block == null) return buildEmptyExpressionBlock()
        if (block.tokenType != BLOCK) {
            return FirSingleExpressionBlock(
                expressionConverter.getAsFirStatement(block)
            )
        }

        return convertBlockExpression(block)
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

    private fun convertDelegationSpecifiers(delegationSpecifiers: LighterASTNode): DelegationSpecifiers {
        val superTypeRefs = mutableListOf<FirTypeRef>()
        val superTypeCalls = mutableListOf<DelegatedConstructorWrapper>()
        val delegateFieldsMap = mutableMapOf<Int, FirFieldSymbol>()
        var index = 0
        delegationSpecifiers.forEachChildren {
            when (it.tokenType) {
                SUPER_TYPE_ENTRY -> {
                    superTypeRefs += convertType(it)
                    index++
                }
                SUPER_TYPE_CALL_ENTRY -> convertConstructorInvocation(it).apply {
                    superTypeCalls += DelegatedConstructorWrapper(first, second, it.toFirSourceElement())
                    superTypeRefs += first
                    index++
                }
                DELEGATED_SUPER_TYPE_ENTRY -> {
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
    private fun convertConstructorInvocation(constructorInvocation: LighterASTNode): Pair<FirTypeRef, List<FirExpression>> {
        var firTypeRef: FirTypeRef = implicitType
        val firValueArguments = mutableListOf<FirExpression>()
        constructorInvocation.forEachChildren {
            when (it.tokenType) {
                CONSTRUCTOR_CALLEE -> firTypeRef = convertType(it)
                VALUE_ARGUMENT_LIST -> firValueArguments += expressionConverter.convertValueArguments(it)
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
        explicitDelegation: LighterASTNode,
        delegateFieldsMap: MutableMap<Int, FirFieldSymbol>,
        index: Int
    ): FirTypeRef {
        lateinit var firTypeRef: FirTypeRef
        var expressionNode: LighterASTNode? = null
        explicitDelegation.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firTypeRef = convertType(it)
                else -> if (it.isExpression()) expressionNode = it
            }
        }

        delegateFieldsMap.put(
            index,
            buildField {
                source = explicitDelegation.toFirSourceElement().fakeElement(KtFakeSourceElementKind.ClassDelegationField)
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Synthetic.DelegateField
                name = NameUtils.delegateFieldName(delegateFieldsMap.size)
                symbol = FirFieldSymbol(CallableId(context.currentClassId, name))
                returnTypeRef = firTypeRef
                withContainerSymbol(symbol) {
                    val errorReason = "Should have delegate"
                    initializer = expressionNode?.let {
                        expressionConverter.getAsFirExpression(it, errorReason)
                    } ?: buildErrorExpression(explicitDelegation.toFirSourceElement(), ConeSyntaxDiagnostic(errorReason))
                }

                isVar = false
                status = FirDeclarationStatusImpl(Visibilities.Private, Modality.FINAL)
                dispatchReceiverType = currentDispatchReceiverType()
            }.symbol
        )
        return firTypeRef
    }

    /*****    TYPES    *****/
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeParameterList
     */
    private fun convertTypeParameters(
        typeParameterList: LighterASTNode,
        typeConstraints: List<TypeConstraint>,
        containingDeclarationSymbol: FirBasedSymbol<*>
    ): List<FirTypeParameter> {
        return typeParameterList.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                TYPE_PARAMETER -> container += convertTypeParameter(node, typeConstraints, containingDeclarationSymbol)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeConstraintList
     */
    private fun convertTypeConstraints(typeConstraints: LighterASTNode): List<TypeConstraint> {
        return typeConstraints.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                TYPE_CONSTRAINT -> container += convertTypeConstraint(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeConstraint
     */
    private fun convertTypeConstraint(typeConstraint: LighterASTNode): TypeConstraint {
        var identifier: String? = null
        var firType: FirTypeRef? = null
        var referenceExpression: LighterASTNode? = null

        val annotations = mutableListOf<FirAnnotation>()
        typeConstraint.forEachChildren {
            when (it.tokenType) {
                ANNOTATION_ENTRY -> {
                    annotations +=
                        convertAnnotationEntry(
                            it,
                            diagnostic = ConeSimpleDiagnostic(
                                "Type parameter annotations are not allowed inside where clauses", DiagnosticKind.AnnotationInWhereClause,
                            )
                        )
                }
                REFERENCE_EXPRESSION -> {
                    identifier = it.asText
                    referenceExpression = it
                }
                TYPE_REFERENCE -> firType = convertType(it)
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
        typeParameter: LighterASTNode,
        typeConstraints: List<TypeConstraint>,
        containingSymbol: FirBasedSymbol<*>
    ): FirTypeParameter {
        var typeParameterModifiers: TypeParameterModifierList? = null
        var identifier: String? = null
        var firType: FirTypeRef? = null
        typeParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> typeParameterModifiers = convertTypeParameterModifiers(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_REFERENCE -> firType = convertType(it)
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
    fun convertType(type: LighterASTNode): FirTypeRef {
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
        // 2. `(suspend @A () -> Int)?` is a nullable suspend function type but the modifier list is on the child NULLABLE_TYPE
        //
        val allTypeModifiers = mutableListOf<ModifierList>()

        var firType: FirTypeRef? = null
        type.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firType = convertType(it)
                MODIFIER_LIST -> allTypeModifiers += convertModifierList(it)
                USER_TYPE -> firType = convertUserType(typeRefSource, it)
                NULLABLE_TYPE -> firType = convertNullableType(typeRefSource, it, allTypeModifiers)
                FUNCTION_TYPE -> firType = convertFunctionType(typeRefSource, it, allTypeModifiers)
                DYNAMIC_TYPE -> firType = buildDynamicTypeRef {
                    source = typeRefSource
                    isMarkedNullable = false
                }
                INTERSECTION_TYPE -> firType = convertIntersectionType(typeRefSource, it, false)
                CONTEXT_RECEIVER_LIST, TokenType.ERROR_ELEMENT -> firType =
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

    private fun convertIntersectionType(typeRefSource: KtSourceElement, intersectionType: LighterASTNode, isNullable: Boolean): FirTypeRef {
        val children = arrayListOf<FirTypeRef>()
        intersectionType.forEachChildren {
            if (it.tokenType != AND) { //skip in forEachChildren?
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
    private fun convertReceiverType(receiverType: LighterASTNode): FirTypeRef {
        receiverType.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> return convertType(it)
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
        nullableType: LighterASTNode,
        allTypeModifiers: MutableList<ModifierList>,
        isNullable: Boolean = true
    ): FirTypeRef {
        lateinit var firType: FirTypeRef
        nullableType.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> allTypeModifiers += convertModifierList(it)
                USER_TYPE -> firType = convertUserType(typeRefSource, it, isNullable)
                FUNCTION_TYPE -> firType = convertFunctionType(typeRefSource, it, allTypeModifiers, isNullable)
                NULLABLE_TYPE -> firType = convertNullableType(typeRefSource, it, allTypeModifiers)
                DYNAMIC_TYPE -> firType = buildDynamicTypeRef {
                    source = typeRefSource
                    isMarkedNullable = true
                }
                INTERSECTION_TYPE -> firType = convertIntersectionType(typeRefSource, it, isNullable)
            }
        }

        return firType
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseUserType
     */
    private fun convertUserType(
        typeRefSource: KtSourceElement,
        userType: LighterASTNode,
        isNullable: Boolean = false
    ): FirTypeRef {
        var simpleFirUserType: FirUserTypeRef? = null
        var identifier: String? = null
        var identifierSource: KtSourceElement? = null
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        var typeArgumentsSource: KtSourceElement? = null
        userType.forEachChildren {
            when (it.tokenType) {
                USER_TYPE -> simpleFirUserType = convertUserType(typeRefSource, it) as? FirUserTypeRef //simple user type
                REFERENCE_EXPRESSION -> {
                    identifierSource = it.toFirSourceElement()
                    identifier = it.asText
                }
                TYPE_ARGUMENT_LIST -> {
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
    fun convertTypeArguments(typeArguments: LighterASTNode, allowedUnderscoredTypeArgument: Boolean): List<FirTypeProjection> {
        return typeArguments.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                TYPE_PROJECTION -> container += convertTypeProjection(node, allowedUnderscoredTypeArgument)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.tryParseTypeArgumentList
     */
    private fun convertTypeProjection(typeProjection: LighterASTNode, allowedUnderscoredTypeArgument: Boolean): FirTypeProjection {
        var modifiers: TypeProjectionModifierList? = null
        lateinit var firType: FirTypeRef
        var isStarProjection = false
        typeProjection.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertTypeArgumentModifierList(it)
                TYPE_REFERENCE -> firType = convertType(it)
                MUL -> isStarProjection = true
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
            val text = qualifierSource.lighterASTNode.getChildNodeByType(IDENTIFIER)?.asText
            return text == "_"
        }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionType
     */
    private fun convertFunctionType(
        typeRefSource: KtSourceElement,
        functionType: LighterASTNode,
        allTypeModifiers: List<ModifierList>,
        isNullable: Boolean = false,
    ): FirTypeRef {
        var receiverTypeReference: FirTypeRef? = null
        lateinit var returnTypeReference: FirTypeRef
        val parameters = mutableListOf<FirFunctionTypeParameter>()
        var contextList: LighterASTNode? = null
        functionType.forEachChildren {
            when (it.tokenType) {
                FUNCTION_TYPE_RECEIVER -> receiverTypeReference = convertReceiverType(it)
                VALUE_PARAMETER_LIST -> parameters += convertFunctionTypeParameters(it)
                TYPE_REFERENCE -> returnTypeReference = convertType(it)
                CONTEXT_RECEIVER_LIST -> contextList = it
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
                when (it.elementType) {
                    CONTEXT_RECEIVER, VALUE_PARAMETER -> {
                        val typeReference = it.getChildNodeByType(TYPE_REFERENCE)

                        contextParameterTypeRefs += typeReference?.let(this@LightTreeRawFirDeclarationBuilder::convertType)
                            ?: buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Type missing") }
                    }
                }
            }
        }
    }

    private fun convertFunctionTypeParameters(
        parameters: LighterASTNode,
    ): List<FirFunctionTypeParameter> {
        return parameters.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_PARAMETER -> {
                    var name: Name? = null
                    var typeRef: FirTypeRef? = null
                    node.forEachChildren {
                        when (it.tokenType) {
                            IDENTIFIER -> name = it.asText.nameAsSafeName()
                            TYPE_REFERENCE -> typeRef = convertType(it)
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
        valueParameters: LighterASTNode,
        functionSymbol: FirFunctionSymbol<*>,
        valueParameterDeclaration: ValueParameterDeclaration,
        additionalAnnotations: List<FirAnnotation> = emptyList()
    ): List<ValueParameter> {
        return valueParameters.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_PARAMETER -> container += convertValueParameter(
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
        valueParameter: LighterASTNode,
        containingDeclarationSymbol: FirBasedSymbol<*>?,
        valueParameterDeclaration: ValueParameterDeclaration,
        additionalAnnotations: List<FirAnnotation> = emptyList()
    ): ValueParameter {
        var modifiers: ModifierList? = null
        var isVal = false
        var isVar = false
        var identifier: String? = null
        var firType: FirTypeRef? = null
        var firExpression: FirExpression? = null
        var destructuringDeclaration: DestructuringDeclaration? = null
        valueParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                VAL_KEYWORD -> isVal = true
                VAR_KEYWORD -> isVar = true
                IDENTIFIER -> identifier = it.asText
                TYPE_REFERENCE -> {}
                DESTRUCTURING_DECLARATION -> destructuringDeclaration = convertDestructingDeclaration(it)
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have default value")
            }
        }

        val name = convertValueParameterName(identifier.nameAsSafeName(), valueParameterDeclaration) { identifier }
        val valueParameterSymbol = FirValueParameterSymbol(name)
        withContainerSymbol(valueParameterSymbol, isLocal = !valueParameterDeclaration.isAnnotationOwner) {
            valueParameter.forEachChildren {
                when (it.tokenType) {
                    TYPE_REFERENCE -> firType = convertType(it)
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
        contextLists: List<LighterASTNode>?,
        containingDeclarationSymbol: FirBasedSymbol<*>,
    ) {
        if (contextLists == null) return
        for (contextList in contextLists) {
            contextList.getChildNodesByType(VALUE_PARAMETER).mapTo(this) { contextParameterElement ->
                convertValueParameter(
                    valueParameter = contextParameterElement,
                    containingDeclarationSymbol = containingDeclarationSymbol,
                    valueParameterDeclaration = ValueParameterDeclaration.CONTEXT_PARAMETER
                ).firValueParameter
            }

            // Legacy context receivers
            contextList.getChildNodesByType(CONTEXT_RECEIVER).mapTo(this) { contextReceiverElement ->
                buildValueParameter {
                    this.source = contextReceiverElement.toFirSourceElement()
                    this.moduleData = baseModuleData
                    this.origin = FirDeclarationOrigin.Source

                    val customLabelName =
                        contextReceiverElement
                            .getChildNodeByType(LABEL_QUALIFIER)
                            ?.getChildNodeByType(LABEL)
                            ?.getChildNodeByType(IDENTIFIER)
                            ?.getReferencedNameAsName()

                    val typeReference = contextReceiverElement.getChildNodeByType(TYPE_REFERENCE)

                    val labelNameFromTypeRef = typeReference?.getChildNodeByType(USER_TYPE)
                        ?.getChildNodeByType(REFERENCE_EXPRESSION)
                        ?.getReferencedNameAsName()

                    // We're abusing the value parameter name for the label/type name of legacy context receivers.
                    // Luckily, legacy context receivers are getting removed soon.
                    this.name = customLabelName ?: labelNameFromTypeRef ?: SpecialNames.UNDERSCORE_FOR_UNUSED_VAR

                    this.symbol = FirValueParameterSymbol(name)
                    withContainerSymbol(this.symbol) {
                        this.returnTypeRef = typeReference?.let { convertType(it) }
                            ?: buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Type missing") }
                    }
                    this.containingDeclarationSymbol = containingDeclarationSymbol
                    this.valueParameterKind = FirValueParameterKind.LegacyContextReceiver
                }
            }
        }
    }
}
