/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.extractContractDescriptionIfPossible
import org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.fir.*
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeModifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeParameterModifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeProjectionModifier
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirReferencePlaceholderForResolvedAnnotations
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class DeclarationsConverter(
    session: FirSession,
    private val baseScopeProvider: FirScopeProvider,
    private val stubMode: Boolean,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    offset: Int = 0,
    context: Context<LighterASTNode> = Context()
) : BaseConverter(session, tree, offset, context) {
    private val expressionConverter = ExpressionsConverter(session, stubMode, tree, this, offset + 1, context)

    /**
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parseFile]
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parsePreamble]
     */
    fun convertFile(file: LighterASTNode, fileName: String = ""): FirFile {
        if (file.tokenType != KT_FILE) {
            //TODO throw error
            throw Exception()
        }

        val fileAnnotationList = mutableListOf<FirAnnotationCall>()
        val importList = mutableListOf<FirImport>()
        val firDeclarationList = mutableListOf<FirDeclaration>()
        context.packageFqName = FqName.ROOT
        file.forEachChildren {
            when (it.tokenType) {
                FILE_ANNOTATION_LIST -> fileAnnotationList += convertFileAnnotationList(it)
                PACKAGE_DIRECTIVE -> context.packageFqName = convertPackageName(it)
                IMPORT_LIST -> importList += convertImportDirectives(it)
                CLASS -> firDeclarationList += convertClass(it)
                FUN -> firDeclarationList += convertFunctionDeclaration(it)
                PROPERTY -> firDeclarationList += convertPropertyDeclaration(it)
                TYPEALIAS -> firDeclarationList += convertTypeAlias(it)
                OBJECT_DECLARATION -> firDeclarationList += convertClass(it)
            }
        }

        return buildFile {
            source = file.toFirSourceElement()
            origin = FirDeclarationOrigin.Source
            session = baseSession
            name = fileName
            packageFqName = context.packageFqName
            annotations += fileAnnotationList
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

    fun convertBlockExpressionWithoutBuilding(block: LighterASTNode): FirBlockBuilder {
        val firStatements = block.forEachChildrenReturnList<FirStatement> { node, container ->
            when (node.tokenType) {
                CLASS, OBJECT_DECLARATION -> container += convertClass(node) as FirStatement
                FUN -> container += convertFunctionDeclaration(node) as FirStatement
                PROPERTY -> container += convertPropertyDeclaration(node) as FirStatement
                DESTRUCTURING_DECLARATION -> container += convertDestructingDeclaration(node).toFirDestructingDeclaration(baseSession)
                TYPEALIAS -> container += convertTypeAlias(node) as FirStatement
                CLASS_INITIALIZER -> container += convertAnonymousInitializer(node) as FirStatement
                else -> if (node.isExpression()) container += expressionConverter.getAsFirExpression<FirStatement>(node)
            }
        }
        return FirBlockBuilder().apply {
            source = block.toFirSourceElement()
            firStatements.forEach { firStatement ->
                if (firStatement !is FirBlock || firStatement.annotations.isNotEmpty()) {
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
    private fun convertPackageName(packageNode: LighterASTNode): FqName {
        var packageName: FqName = FqName.ROOT
        packageNode.forEachChildren {
            when (it.tokenType) {
                //TODO separate logic for both expression types
                DOT_QUALIFIED_EXPRESSION, REFERENCE_EXPRESSION -> packageName = FqName(it.getAsStringWithoutBacktick())
            }
        }
        return packageName
    }

    private fun convertImportAlias(importAlias: LighterASTNode): String? {
        importAlias.forEachChildren {
            when (it.tokenType) {
                IDENTIFIER -> return it.asText
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
        importDirective.forEachChildren {
            when (it.tokenType) {
                DOT_QUALIFIED_EXPRESSION, REFERENCE_EXPRESSION -> importedFqName = FqName(it.asText)
                MUL -> isAllUnder = true
                IMPORT_ALIAS -> aliasName = convertImportAlias(it)
            }
        }

        return buildImport {
            source = importDirective.toFirSourceElement()
            this.importedFqName = importedFqName
            this.isAllUnder = isAllUnder
            this.aliasName = aliasName?.let { Name.identifier(it) }
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
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertModifierList(modifiers: LighterASTNode, isInClass: Boolean = false): Modifier {
        val modifier = Modifier()
        modifiers.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> modifier.annotations += convertAnnotation(it)
                ANNOTATION_ENTRY -> modifier.annotations += convertAnnotationEntry(it)
                is KtModifierKeywordToken -> modifier.addModifier(it, isInClass)
            }
        }
        return modifier
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeModifierList
     */
    private fun convertTypeModifierList(modifiers: LighterASTNode): TypeModifier {
        val typeModifierList = TypeModifier()
        modifiers.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> typeModifierList.annotations += convertAnnotation(it)
                ANNOTATION_ENTRY -> typeModifierList.annotations += convertAnnotationEntry(it)
                is KtModifierKeywordToken -> typeModifierList.addModifier(it)
            }
        }
        return typeModifierList
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentModifierList
     */
    private fun convertTypeArgumentModifierList(modifiers: LighterASTNode): TypeProjectionModifier {
        val typeArgumentModifierList = TypeProjectionModifier()
        modifiers.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> typeArgumentModifierList.annotations += convertAnnotation(it)
                ANNOTATION_ENTRY -> typeArgumentModifierList.annotations += convertAnnotationEntry(it)
                is KtModifierKeywordToken -> typeArgumentModifierList.addModifier(it)
            }
        }
        return typeArgumentModifierList
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentModifierList
     */
    private fun convertTypeParameterModifiers(modifiers: LighterASTNode): TypeParameterModifier {
        val modifier = TypeParameterModifier()
        modifiers.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> modifier.annotations += convertAnnotation(it)
                ANNOTATION_ENTRY -> modifier.annotations += convertAnnotationEntry(it)
                is KtModifierKeywordToken -> modifier.addModifier(it)
            }
        }
        return modifier
    }

    /*****    ANNOTATIONS    *****/
    /**
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parseFileAnnotationList]
     */
    private fun convertFileAnnotationList(fileAnnotationList: LighterASTNode): List<FirAnnotationCall> {
        return fileAnnotationList.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                ANNOTATION -> container += convertAnnotation(node)
                ANNOTATION_ENTRY -> container += convertAnnotationEntry(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseAnnotationOrList
     */
    fun convertAnnotation(annotationNode: LighterASTNode): List<FirAnnotationCall> {
        var annotationTarget: AnnotationUseSiteTarget? = null
        return annotationNode.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                ANNOTATION_TARGET -> annotationTarget = convertAnnotationTarget(node)
                ANNOTATION_ENTRY -> container += convertAnnotationEntry(node, annotationTarget)
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
                FIELD_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.FIELD
                FILE_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.FILE
                PROPERTY_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.PROPERTY
                GET_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.PROPERTY_GETTER
                SET_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.PROPERTY_SETTER
                RECEIVER_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.RECEIVER
                PARAM_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
                SETPARAM_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.SETTER_PARAMETER
                DELEGATE_KEYWORD -> annotationTarget = AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
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
        defaultAnnotationUseSiteTarget: AnnotationUseSiteTarget? = null
    ): FirAnnotationCall {
        var annotationUseSiteTarget: AnnotationUseSiteTarget? = null
        lateinit var constructorCalleePair: Pair<FirTypeRef, List<FirExpression>>
        unescapedAnnotation.forEachChildren {
            when (it.tokenType) {
                ANNOTATION_TARGET -> annotationUseSiteTarget = convertAnnotationTarget(it)
                CONSTRUCTOR_CALLEE -> constructorCalleePair = convertConstructorInvocation(unescapedAnnotation)
            }
        }
        val name = (constructorCalleePair.first as? FirUserTypeRef)?.qualifier?.last()?.name ?: Name.special("<no-annotation-name>")
        return buildAnnotationCall {
            source = unescapedAnnotation.toFirSourceElement()
            useSiteTarget = annotationUseSiteTarget ?: defaultAnnotationUseSiteTarget
            annotationTypeRef = constructorCalleePair.first
            calleeReference = buildSimpleNamedReference {
                source = this@buildAnnotationCall.source
                this.name = name
            }
            extractArgumentsFrom(constructorCalleePair.second, stubMode)
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
        var modifiers = Modifier()
        var classKind: ClassKind = ClassKind.CLASS //TODO
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
                CLASS_KEYWORD -> classKind = ClassKind.CLASS
                INTERFACE_KEYWORD -> classKind = ClassKind.INTERFACE
                OBJECT_KEYWORD -> classKind = ClassKind.OBJECT
                IDENTIFIER -> identifier = it.asText
                TYPE_PARAMETER_LIST -> typeParameterList = it
                PRIMARY_CONSTRUCTOR -> primaryConstructor = it
                SUPER_TYPE_LIST -> superTypeList = it
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                CLASS_BODY -> classBody = it
            }
        }
        typeParameterList?.let { firTypeParameters += convertTypeParameters(it, typeConstraints) }

        if (classKind == ClassKind.CLASS) {
            classKind = when {
                modifiers.isEnum() -> ClassKind.ENUM_CLASS
                modifiers.isAnnotation() -> ClassKind.ANNOTATION_CLASS
                else -> classKind
            }
        }

        val className = identifier.nameAsSafeName(if (modifiers.isCompanion()) "Companion" else "")
        val isLocal = isClassLocal(classNode) { getParent() }

        return withChildClassName(className, isLocal) {
            withCapturedTypeParameters {
                val status = FirDeclarationStatusImpl(
                    if (isLocal) Visibilities.Local else modifiers.getVisibility(),
                    modifiers.getModality()
                ).apply {
                    isExpect = modifiers.hasExpect()
                    isActual = modifiers.hasActual()
                    isInner = modifiers.isInner()
                    isCompanion = modifiers.isCompanion() && classKind == ClassKind.OBJECT
                    isData = modifiers.isDataClass()
                    isInline = modifiers.isInlineClass()
                    isFun = modifiers.isFunctionalInterface()
                }

                buildRegularClass {
                    source = classNode.toFirSourceElement()
                    session = baseSession
                    origin = FirDeclarationOrigin.Source
                    name = className
                    this.status = status
                    this.classKind = classKind
                    scopeProvider = baseScopeProvider
                    symbol = FirRegularClassSymbol(context.currentClassId)
                    annotations += modifiers.annotations
                    typeParameters += firTypeParameters

                    if (!status.isInner) clearCapturedTypeParameters()
                    typeParameters += context.capturedTypeParameters.map { buildOuterClassTypeParameterRef { symbol = it } }
                    addCapturedTypeParameters(firTypeParameters)

                    val selfType = classNode.toDelegatedSelfType(this)


                    val delegationSpecifiers = superTypeList?.let { convertDelegationSpecifiers(it, symbol, selfType) }
                    var delegatedSuperTypeRef: FirTypeRef? = delegationSpecifiers?.delegatedSuperTypeRef
                    val delegatedConstructorSource: FirLightSourceElement? = delegationSpecifiers?.delegatedConstructorSource
                    delegationSpecifiers?.delegateFields?.map { declarations += it }

                    val superTypeCallEntry = delegationSpecifiers?.delegatedConstructorArguments.orEmpty()
                    val superTypeRefs = mutableListOf<FirTypeRef>()

                    delegationSpecifiers?.let { superTypeRefs += it.superTypesRef }

                    when {
                        modifiers.isEnum() && (classKind == ClassKind.ENUM_CLASS) -> {
                            delegatedSuperTypeRef = buildResolvedTypeRef {
                                type = ConeClassLikeTypeImpl(
                                    implicitEnumType.type.lookupTag,
                                    arrayOf(selfType.type),
                                    isNullable = false
                                )
                            }
                            superTypeRefs += delegatedSuperTypeRef
                        }
                        modifiers.isAnnotation() && (classKind == ClassKind.ANNOTATION_CLASS) -> {
                            superTypeRefs += implicitAnnotationType
                            delegatedSuperTypeRef = implicitAnyType
                        }
                    }

                    superTypeRefs.ifEmpty {
                        superTypeRefs += implicitAnyType
                        delegatedSuperTypeRef = implicitAnyType
                    }

                    this.superTypeRefs += superTypeRefs

                    val secondaryConstructors = classBody.getChildNodesByType(SECONDARY_CONSTRUCTOR)
                    val classWrapper = ClassWrapper(
                        className, modifiers, classKind, this,
                        hasPrimaryConstructor = primaryConstructor != null,
                        hasSecondaryConstructor = secondaryConstructors.isNotEmpty(),
                        hasDefaultConstructor = if (primaryConstructor != null) !primaryConstructor!!.hasValueParameters()
                        else secondaryConstructors.isEmpty() || secondaryConstructors.any { !it.hasValueParameters() },
                        delegatedSelfTypeRef = selfType,
                        delegatedSuperTypeRef = delegatedSuperTypeRef ?: buildImplicitTypeRef(),
                        superTypeCallEntry = superTypeCallEntry
                    )
                    //parse primary constructor
                    val primaryConstructorWrapper = convertPrimaryConstructor(
                        primaryConstructor, selfType.source, classWrapper, delegatedConstructorSource,
                        delegationSpecifiers?.primaryConstructorBody
                    )
                    val firPrimaryConstructor = primaryConstructorWrapper?.firConstructor
                    firPrimaryConstructor?.let { declarations += it }

                    val properties = mutableListOf<FirProperty>()
                    if (primaryConstructor != null && firPrimaryConstructor != null) {
                        //parse properties
                        properties += primaryConstructorWrapper.valueParameters
                            .filter { it.hasValOrVar() }
                            .map { it.toFirProperty(baseSession, callableIdForName(it.firValueParameter.name), classWrapper.hasExpect()) }
                        addDeclarations(properties)
                    }

                    //parse declarations
                    classBody?.let {
                        addDeclarations(convertClassBody(it, classWrapper))
                    }

                    //parse data class
                    if (modifiers.isDataClass() && firPrimaryConstructor != null) {
                        val zippedParameters = properties.map { it.source?.lightNode!! to it }
                        DataClassMembersGenerator(
                            baseSession,
                            classNode,
                            this,
                            zippedParameters,
                            context.packageFqName,
                            context.className,
                            createClassTypeRefWithSourceKind = { firPrimaryConstructor.returnTypeRef },
                            createParameterTypeRefWithSourceKind = { property, _ -> property.returnTypeRef },
                        ).generate()
                    }

                    if (modifiers.isEnum()) {
                        generateValuesFunction(baseSession, context.packageFqName, context.className, modifiers.hasExpect())
                        generateValueOfFunction(baseSession, context.packageFqName, context.className, modifiers.hasExpect())
                    }
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseObjectLiteral
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitObjectLiteralExpression
     */
    fun convertObjectLiteral(objectLiteral: LighterASTNode): FirElement {
        return withChildClassName(ANONYMOUS_OBJECT_NAME) {
            buildAnonymousObject {
                source = objectLiteral.toFirSourceElement()
                origin = FirDeclarationOrigin.Source
                session = baseSession
                classKind = ClassKind.OBJECT
                scopeProvider = baseScopeProvider
                symbol = FirAnonymousObjectSymbol()
                typeParameters += context.capturedTypeParameters.map { buildOuterClassTypeParameterRef { this.symbol = it } }
                val delegatedSelfType = objectLiteral.toDelegatedSelfType(this)

                var modifiers = Modifier()
                var primaryConstructor: LighterASTNode? = null
                val superTypeRefs = mutableListOf<FirTypeRef>()
                val superTypeCallEntry = mutableListOf<FirExpression>()
                var delegatedSuperTypeRef: FirTypeRef? = null
                var classBody: LighterASTNode? = null
                var delegatedConstructorSource: FirLightSourceElement? = null
                var delegateFields: List<FirField>? = null
                var primaryConstructorBody: FirBlock? = null

                objectLiteral.getChildNodesByType(OBJECT_DECLARATION).first().forEachChildren {
                    when (it.tokenType) {
                        MODIFIER_LIST -> modifiers = convertModifierList(it)
                        PRIMARY_CONSTRUCTOR -> primaryConstructor = it
                        SUPER_TYPE_LIST -> convertDelegationSpecifiers(it, symbol, delegatedSelfType).let {
                            delegatedSuperTypeRef = it.delegatedSuperTypeRef
                            superTypeRefs += it.superTypesRef
                            superTypeCallEntry += it.delegatedConstructorArguments
                            delegatedConstructorSource = it.delegatedConstructorSource
                            delegateFields = it.delegateFields
                            primaryConstructorBody = it.primaryConstructorBody
                        }
                        CLASS_BODY -> classBody = it
                    }
                }

                superTypeRefs.ifEmpty {
                    superTypeRefs += implicitAnyType
                    delegatedSuperTypeRef = implicitAnyType
                }
                val delegatedSuperType = delegatedSuperTypeRef ?: buildImplicitTypeRef()

                annotations += modifiers.annotations
                this.superTypeRefs += superTypeRefs
                typeRef = delegatedSelfType

                delegateFields?.map { this.declarations += it }
                val classWrapper = ClassWrapper(
                    SpecialNames.NO_NAME_PROVIDED, modifiers, ClassKind.OBJECT, this,
                    hasPrimaryConstructor = false,
                    hasSecondaryConstructor = classBody.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                    hasDefaultConstructor = false,
                    delegatedSelfTypeRef = delegatedSelfType,
                    delegatedSuperTypeRef = delegatedSuperType,
                    superTypeCallEntry = superTypeCallEntry
                )
                //parse primary constructor
                convertPrimaryConstructor(primaryConstructor, typeRef.source, classWrapper, delegatedConstructorSource, primaryConstructorBody)
                    ?.let { this.declarations += it.firConstructor }

                //parse declarations
                classBody?.let {
                    this.declarations += convertClassBody(it, classWrapper)
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumEntry
     */
    private fun convertEnumEntry(enumEntry: LighterASTNode, classWrapper: ClassWrapper): FirEnumEntry {
        var modifiers = Modifier()
        lateinit var identifier: String
        val enumSuperTypeCallEntry = mutableListOf<FirExpression>()
        var classBodyNode: LighterASTNode? = null
        enumEntry.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                INITIALIZER_LIST -> enumSuperTypeCallEntry += convertInitializerList(it)
                CLASS_BODY -> classBodyNode = it
            }
        }

        val enumEntryName = identifier.nameAsSafeName()
        return buildEnumEntry {
            source = enumEntry.toFirSourceElement()
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = classWrapper.delegatedSelfTypeRef
            name = enumEntryName
            symbol = FirVariableSymbol(CallableId(context.currentClassId, enumEntryName))
            status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                isStatic = true
                isExpect = classWrapper.hasExpect()
            }
            if (classWrapper.hasDefaultConstructor && enumEntry.getChildNodeByType(INITIALIZER_LIST) == null &&
                modifiers.annotations.isEmpty() && classBodyNode == null
            ) {
                return@buildEnumEntry
            }
            annotations += modifiers.annotations
            initializer = withChildClassName(enumEntryName) {
                buildAnonymousObject {
                    source = this@buildEnumEntry.source
                    session = baseSession
                    origin = FirDeclarationOrigin.Source
                    classKind = ClassKind.ENUM_ENTRY
                    scopeProvider = baseScopeProvider
                    symbol = FirAnonymousObjectSymbol()
                    annotations += modifiers.annotations
                    val enumClassWrapper = ClassWrapper(
                        enumEntryName, modifiers, ClassKind.ENUM_ENTRY, this,
                        hasPrimaryConstructor = true,
                        hasSecondaryConstructor = classBodyNode.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                        hasDefaultConstructor = false,
                        delegatedSelfTypeRef = buildResolvedTypeRef {
                            type = ConeClassLikeTypeImpl(
                                this@buildAnonymousObject.symbol.toLookupTag(),
                                emptyArray(),
                                isNullable = false
                            )
                        },
                        delegatedSuperTypeRef = classWrapper.delegatedSelfTypeRef,
                        superTypeCallEntry = enumSuperTypeCallEntry
                    )
                    superTypeRefs += enumClassWrapper.delegatedSuperTypeRef
                    convertPrimaryConstructor(null, null, enumClassWrapper, null)?.let { declarations += it.firConstructor }
                    classBodyNode?.also { declarations += convertClassBody(it, enumClassWrapper) }
                }
            }
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
        return classBody.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                ENUM_ENTRY -> container += convertEnumEntry(node, classWrapper)
                CLASS -> container += convertClass(node)
                FUN -> container += convertFunctionDeclaration(node, classWrapper)
                PROPERTY -> container += convertPropertyDeclaration(node, classWrapper)
                TYPEALIAS -> container += convertTypeAlias(node)
                OBJECT_DECLARATION -> container += convertClass(node)
                CLASS_INITIALIZER -> container += convertAnonymousInitializer(node) //anonymousInitializer
                SECONDARY_CONSTRUCTOR -> container += convertSecondaryConstructor(node, classWrapper)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassOrObject
     * primaryConstructor branch
     */
    private fun convertPrimaryConstructor(
        primaryConstructor: LighterASTNode?,
        selfTypeSource: FirSourceElement?,
        classWrapper: ClassWrapper,
        delegatedConstructorSource: FirLightSourceElement?,
        body: FirBlock? = null
    ): PrimaryConstructor? {
        if (primaryConstructor == null && !classWrapper.isEnumEntry() && classWrapper.hasSecondaryConstructor) return null
        if (classWrapper.isInterface()) return null

        var modifiers = Modifier()
        val valueParameters = mutableListOf<ValueParameter>()
        primaryConstructor?.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                VALUE_PARAMETER_LIST -> valueParameters += convertValueParameters(it)
            }
        }

        val defaultVisibility = classWrapper.defaultConstructorVisibility()
        val firDelegatedCall = buildDelegatedConstructorCall {
            source = delegatedConstructorSource ?: selfTypeSource?.fakeElement(FirFakeSourceElementKind.DelegatingConstructorCall)
            constructedTypeRef = classWrapper.delegatedSuperTypeRef.copyWithNewSourceKind(FirFakeSourceElementKind.ImplicitTypeRef)
            isThis = false
            extractArgumentsFrom(classWrapper.superTypeCallEntry, stubMode)
        }

        val explicitVisibility = if (primaryConstructor != null) modifiers.getVisibility() else null
        val status = FirDeclarationStatusImpl(explicitVisibility ?: defaultVisibility, Modality.FINAL).apply {
            isExpect = modifiers.hasExpect() || classWrapper.hasExpect()
            isActual = modifiers.hasActual()
            isInner = classWrapper.isInner()
            isFromSealedClass = classWrapper.isSealed() && explicitVisibility !== Visibilities.Private
            isFromEnumClass = classWrapper.isEnum()
        }

        return PrimaryConstructor(
            buildPrimaryConstructor {
                source = primaryConstructor?.toFirSourceElement()
                    ?: selfTypeSource?.fakeElement(FirFakeSourceElementKind.ImplicitConstructor)
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = classWrapper.delegatedSelfTypeRef
                this.status = status
                symbol = FirConstructorSymbol(callableIdForClassConstructor())
                annotations += modifiers.annotations
                typeParameters += constructorTypeParametersFromConstructedClass(classWrapper.classBuilder.typeParameters)
                this.valueParameters += valueParameters.map { it.firValueParameter }
                delegatedConstructor = firDelegatedCall
                this.body = body
            }, valueParameters
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseMemberDeclarationRest
     * at INIT keyword
     */
    private fun convertAnonymousInitializer(anonymousInitializer: LighterASTNode): FirDeclaration {
        var firBlock: FirBlock? = null
        anonymousInitializer.forEachChildren {
            when (it.tokenType) {
                BLOCK -> firBlock = convertBlock(it)
            }
        }

        return buildAnonymousInitializer {
            source = anonymousInitializer.toFirSourceElement()
            session = baseSession
            origin = FirDeclarationOrigin.Source
            body = if (stubMode) buildEmptyExpressionBlock() else firBlock ?: buildEmptyExpressionBlock()
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseSecondaryConstructor
     */
    private fun convertSecondaryConstructor(secondaryConstructor: LighterASTNode, classWrapper: ClassWrapper): FirConstructor {
        var modifiers = Modifier()
        val firValueParameters = mutableListOf<ValueParameter>()
        var constructorDelegationCall: FirDelegatedConstructorCall? = null
        var block: LighterASTNode? = null

        secondaryConstructor.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                VALUE_PARAMETER_LIST -> firValueParameters += convertValueParameters(it)
                CONSTRUCTOR_DELEGATION_CALL -> constructorDelegationCall = convertConstructorDelegationCall(it, classWrapper)
                BLOCK -> block = it
            }
        }

        val delegatedSelfTypeRef = classWrapper.delegatedSelfTypeRef

        val explicitVisibility = modifiers.getVisibility()
        val status = FirDeclarationStatusImpl(explicitVisibility, Modality.FINAL).apply {
            isExpect = modifiers.hasExpect() || classWrapper.hasExpect()
            isActual = modifiers.hasActual()
            isInner = classWrapper.isInner()
            isFromSealedClass = classWrapper.isSealed() && explicitVisibility !== Visibilities.Private
            isFromEnumClass = classWrapper.isEnum()
        }

        val target = FirFunctionTarget(labelName = null, isLambda = false)
        return buildConstructor {
            source = secondaryConstructor.toFirSourceElement()
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = delegatedSelfTypeRef
            this.status = status
            symbol = FirConstructorSymbol(callableIdForClassConstructor())
            delegatedConstructor = constructorDelegationCall

            context.firFunctionTargets += target
            annotations += modifiers.annotations
            typeParameters += constructorTypeParametersFromConstructedClass(classWrapper.classBuilder.typeParameters)
            valueParameters += firValueParameters.map { it.firValueParameter }
            val (body, _) = convertFunctionBody(block, null)
            this.body = body
            context.firFunctionTargets.removeLast()
        }.also {
            target.bind(it)
        }
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.convert(
     * KtConstructorDelegationCall, FirTypeRef, Boolean)
     */
    private fun convertConstructorDelegationCall(
        constructorDelegationCall: LighterASTNode,
        classWrapper: ClassWrapper
    ): FirDelegatedConstructorCall {
        var thisKeywordPresent = false
        val firValueArguments = mutableListOf<FirExpression>()
        constructorDelegationCall.forEachChildren {
            when (it.tokenType) {
                CONSTRUCTOR_DELEGATION_REFERENCE -> if (it.asText == "this") thisKeywordPresent = true
                VALUE_ARGUMENT_LIST -> firValueArguments += expressionConverter.convertValueArguments(it)
            }
        }

        val isImplicit = constructorDelegationCall.asText.isEmpty()
        val isThis = thisKeywordPresent //|| (isImplicit && classWrapper.hasPrimaryConstructor)
        val delegatedType =
            when {
                isThis -> classWrapper.delegatedSelfTypeRef
                else -> classWrapper.delegatedSuperTypeRef
            }

        return buildDelegatedConstructorCall {
            source = if (isImplicit) {
                constructorDelegationCall.toFirSourceElement().fakeElement(FirFakeSourceElementKind.ImplicitConstructor)
            } else {
                constructorDelegationCall.toFirSourceElement()
            }
            constructedTypeRef = delegatedType.copyWithNewSourceKind(FirFakeSourceElementKind.ImplicitTypeRef)
            this.isThis = isThis
            extractArgumentsFrom(firValueArguments, stubMode)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeAlias
     */
    private fun convertTypeAlias(typeAlias: LighterASTNode): FirDeclaration {
        var modifiers = Modifier()
        var identifier: String? = null
        lateinit var firType: FirTypeRef
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        typeAlias.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it, emptyList())
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val typeAliasName = identifier.nameAsSafeName()
        return withChildClassName(typeAliasName) {
            return@withChildClassName buildTypeAlias {
                source = typeAlias.toFirSourceElement()
                session = baseSession
                origin = FirDeclarationOrigin.Source
                name = typeAliasName
                status = FirDeclarationStatusImpl(modifiers.getVisibility(), Modality.FINAL).apply {
                    isExpect = modifiers.hasExpect()
                    isActual = modifiers.hasActual()
                }
                symbol = FirTypeAliasSymbol(context.currentClassId)
                expandedTypeRef = firType
                annotations += modifiers.annotations
                typeParameters += firTypeParameters
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseProperty
     */
    fun convertPropertyDeclaration(property: LighterASTNode, classWrapper: ClassWrapper? = null): FirDeclaration {
        var modifiers = Modifier()
        var identifier: String? = null
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        var isReturnType = false
        var delegateExpression: LighterASTNode? = null
        var isVar = false
        var receiverType: FirTypeRef? = null
        var returnType: FirTypeRef = implicitType
        val typeConstraints = mutableListOf<TypeConstraint>()
        val accessors = mutableListOf<LighterASTNode>()
        var propertyInitializer: FirExpression? = null
        var typeParameterList: LighterASTNode? = null
        property.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_PARAMETER_LIST -> typeParameterList = it
                COLON -> isReturnType = true
                TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverType = convertType(it)
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                PROPERTY_DELEGATE -> delegateExpression = it
                VAR_KEYWORD -> isVar = true
                PROPERTY_ACCESSOR -> {
                    accessors += it
                }
                else -> if (it.isExpression()) propertyInitializer = expressionConverter.getAsFirExpression(it, "Should have initializer")
            }
        }

        typeParameterList?.let { firTypeParameters += convertTypeParameters(it, typeConstraints) }

        val propertyName = identifier.nameAsSafeName()

        val parentNode = property.getParent()
        val isLocal = !(parentNode?.tokenType == KT_FILE || parentNode?.tokenType == CLASS_BODY)
        val propertySource = property.toFirSourceElement()

        return buildProperty {
            source = propertySource
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = returnType
            name = propertyName
            this.isVar = isVar
            initializer = propertyInitializer

            if (isLocal) {
                this.isLocal = true
                symbol = FirPropertySymbol(propertyName)
                val delegateBuilder = delegateExpression?.let {
                    FirWrappedDelegateExpressionBuilder().apply {
                        source = it.toFirSourceElement()
                        expression = expressionConverter.getAsFirExpression(it, "Incorrect delegate expression")
                    }
                }
                status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL).apply {
                    isLateInit = modifiers.hasLateinit()
                }

                val receiver = delegateExpression?.let {
                    expressionConverter.getAsFirExpression<FirExpression>(it, "Incorrect delegate expression")
                }
                generateAccessorsByDelegate(
                    delegateBuilder,
                    classWrapper?.classBuilder,
                    baseSession,
                    isExtension = false,
                    stubMode,
                    receiver
                )
            } else {
                this.isLocal = false
                receiverTypeRef = receiverType
                symbol = FirPropertySymbol(callableIdForName(propertyName))
                withCapturedTypeParameters {
                    typeParameters += firTypeParameters
                    addCapturedTypeParameters(firTypeParameters)

                    val delegateBuilder = delegateExpression?.let {
                        FirWrappedDelegateExpressionBuilder().apply {
                            source = it.toFirSourceElement()
                            expression = expressionConverter.getAsFirExpression(it, "Should have delegate")
                        }
                    }

                    val propertyVisibility = modifiers.getVisibility()

                    val convertedAccessors = accessors.map { convertGetterOrSetter(it, returnType, propertyVisibility, modifiers) }
                    this.getter = convertedAccessors.find { it.isGetter }
                        ?: FirDefaultPropertyGetter(null, session, FirDeclarationOrigin.Source, returnType, propertyVisibility)
                    this.setter =
                        if (isVar) {
                            convertedAccessors.find { it.isSetter }
                                ?: FirDefaultPropertySetter(null, session, FirDeclarationOrigin.Source, returnType, propertyVisibility)
                        } else null

                    // Upward propagation of `inline` and `external` modifiers (from accessors to property)
                    // Note that, depending on `var` or `val`, checking setter's modifiers should be careful: for `val`, setter doesn't
                    // exist (null); for `var`, the retrieval of the specific modifier is supposed to be `true`
                    status = FirDeclarationStatusImpl(propertyVisibility, modifiers.getModality()).apply {
                        isExpect = modifiers.hasExpect() || classWrapper?.hasExpect() == true
                        isActual = modifiers.hasActual()
                        isOverride = modifiers.hasOverride()
                        isConst = modifiers.isConst()
                        isLateInit = modifiers.hasLateinit()
                        isInline = modifiers.hasInline() || (getter!!.isInline && setter?.isInline != false)
                        isExternal = modifiers.hasExternal() || (getter!!.isExternal && setter?.isExternal != false)
                    }

                    val receiver = delegateExpression?.let {
                        expressionConverter.getAsFirExpression<FirExpression>(it, "Should have delegate")
                    }
                    generateAccessorsByDelegate(
                        delegateBuilder,
                        classWrapper?.classBuilder,
                        baseSession,
                        isExtension = receiverType != null,
                        stubMode,
                        receiver
                    )
                }
            }
            annotations += modifiers.annotations
        }
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitDestructuringDeclaration
     */
    private fun convertDestructingDeclaration(destructingDeclaration: LighterASTNode): DestructuringDeclaration {
        var isVar = false
        val entries = mutableListOf<FirVariable<*>>()
        val source = destructingDeclaration.toFirSourceElement()
        var firExpression: FirExpression =
            buildErrorExpression(null, ConeSimpleDiagnostic("Destructuring declaration without initializer", DiagnosticKind.Syntax))
        destructingDeclaration.forEachChildren {
            when (it.tokenType) {
                VAR_KEYWORD -> isVar = true
                DESTRUCTURING_DECLARATION_ENTRY -> entries += convertDestructingDeclarationEntry(it)
                else -> if (it.isExpression()) firExpression =
                    expressionConverter.getAsFirExpression(it, "Destructuring declaration without initializer")
            }
        }

        return DestructuringDeclaration(isVar, entries, firExpression, source)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseMultiDeclarationName
     */
    private fun convertDestructingDeclarationEntry(entry: LighterASTNode): FirVariable<*> {
        var modifiers = Modifier()
        var identifier: String? = null
        var firType: FirTypeRef? = null
        entry.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val name = identifier.nameAsSafeName()
        return buildProperty {
            source = entry.toFirSourceElement()
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = firType ?: implicitType
            this.name = name
            isVar = false
            symbol = FirPropertySymbol(name)
            isLocal = true
            status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
            annotations += modifiers.annotations
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyGetterOrSetter
     */
    private fun convertGetterOrSetter(
        getterOrSetter: LighterASTNode,
        propertyTypeRef: FirTypeRef,
        propertyVisibility: Visibility,
        propertyModifiers: Modifier
    ): FirPropertyAccessor {
        var modifiers = Modifier()
        var isGetter = true
        var returnType: FirTypeRef? = null
        var firValueParameters: FirValueParameter = buildDefaultSetterValueParameter {
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = propertyTypeRef
            symbol = FirVariableSymbol(NAME_FOR_DEFAULT_VALUE_PARAMETER)
        }
        var block: LighterASTNode? = null
        var expression: LighterASTNode? = null
        var outerContractDescription: FirContractDescription? = null
        getterOrSetter.forEachChildren {
            if (it.asText == "set") isGetter = false
            when (it.tokenType) {
                SET_KEYWORD -> isGetter = false
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                TYPE_REFERENCE -> returnType = convertType(it)
                VALUE_PARAMETER_LIST -> firValueParameters = convertSetterParameter(it, propertyTypeRef)
                CONTRACT_EFFECT_LIST -> outerContractDescription = obtainContractDescription(it)
                BLOCK -> block = it
                else -> if (it.isExpression()) expression = it
            }
        }

        var accessorVisibility = modifiers.getVisibility()
        if (accessorVisibility == Visibilities.Unknown) {
            accessorVisibility = propertyVisibility
        }
        val status =
            // Downward propagation of `inline` and `external` modifiers (from property to its accessors)
            FirDeclarationStatusImpl(accessorVisibility, Modality.FINAL).apply {
                isInline = propertyModifiers.hasInline() || modifiers.hasInline()
                isExternal = propertyModifiers.hasExternal() || modifiers.hasExternal()
            }
        val sourceElement = getterOrSetter.toFirSourceElement()
        if (block == null && expression == null) {
            return FirDefaultPropertyAccessor
                .createGetterOrSetter(
                    sourceElement,
                    baseSession,
                    FirDeclarationOrigin.Source,
                    propertyTypeRef,
                    accessorVisibility,
                    isGetter
                )
                .also {
                    it.annotations += modifiers.annotations
                    it.status = status
                }
        }
        val target = FirFunctionTarget(labelName = null, isLambda = false)
        return buildPropertyAccessor {
            source = sourceElement
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = returnType ?: if (isGetter) propertyTypeRef else implicitUnitType
            symbol = FirPropertyAccessorSymbol()
            this.isGetter = isGetter
            this.status = status
            context.firFunctionTargets += target
            annotations += modifiers.annotations

            if (!isGetter) {
                valueParameters += firValueParameters
            }

            val hasContractEffectList = outerContractDescription != null
            val bodyWithContractDescription = convertFunctionBody(block, expression, hasContractEffectList)
            this.body = bodyWithContractDescription.first
            val contractDescription = outerContractDescription ?: bodyWithContractDescription.second
            contractDescription?.let {
                this.contractDescription = it
            }
            context.firFunctionTargets.removeLast()
        }.also {
            target.bind(it)
        }
    }

    private fun obtainContractDescription(rawContractDescription: LighterASTNode): FirContractDescription? =
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
                        val errorExpression = buildErrorExpression(null, ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionRequired))
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
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyGetterOrSetter
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.toFirValueParameter
     */
    private fun convertSetterParameter(setterParameter: LighterASTNode, propertyTypeRef: FirTypeRef): FirValueParameter {
        var modifiers = Modifier()
        lateinit var firValueParameter: FirValueParameter
        setterParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                VALUE_PARAMETER -> firValueParameter = convertValueParameter(it).firValueParameter
            }
        }

        return buildValueParameter {
            source = setterParameter.toFirSourceElement()
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = if (firValueParameter.returnTypeRef == implicitType) propertyTypeRef else firValueParameter.returnTypeRef
            name = firValueParameter.name
            symbol = FirVariableSymbol(firValueParameter.name)
            defaultValue = firValueParameter.defaultValue
            isCrossinline = modifiers.hasCrossinline() || firValueParameter.isCrossinline
            isNoinline = modifiers.hasNoinline() || firValueParameter.isNoinline
            isVararg = modifiers.hasVararg() || firValueParameter.isVararg
            annotations += modifiers.annotations + firValueParameter.annotations
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunction
     */
    fun convertFunctionDeclaration(functionDeclaration: LighterASTNode, classWrapper: ClassWrapper? = null): FirDeclaration {
        var modifiers = Modifier()
        var identifier: String? = null
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        var valueParametersList: LighterASTNode? = null
        var isReturnType = false
        var receiverType: FirTypeRef? = null
        var returnType: FirTypeRef? = null
        val typeConstraints = mutableListOf<TypeConstraint>()
        var block: LighterASTNode? = null
        var expression: LighterASTNode? = null
        var hasEqToken = false
        var typeParameterList: LighterASTNode? = null
        var outerContractDescription: FirContractDescription? = null
        functionDeclaration.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_PARAMETER_LIST -> typeParameterList = it
                VALUE_PARAMETER_LIST -> valueParametersList = it //must convert later, because it can contains "return"
                COLON -> isReturnType = true
                TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverType = convertType(it)
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                CONTRACT_EFFECT_LIST -> outerContractDescription = obtainContractDescription(it)
                BLOCK -> block = it
                EQ -> hasEqToken = true
                else -> if (it.isExpression()) expression = it
            }
        }
        typeParameterList?.let { firTypeParameters += convertTypeParameters(it, typeConstraints) }

        if (returnType == null) {
            returnType =
                if (block != null || !hasEqToken) implicitUnitType
                else implicitType
        }

        val parentNode = functionDeclaration.getParent()
        val isLocal = !(parentNode?.tokenType == KT_FILE || parentNode?.tokenType == CLASS_BODY)
        val target: FirFunctionTarget
        val functionBuilder = if (identifier == null && isLocal) {
            target = FirFunctionTarget(labelName = functionDeclaration.getLabelName(), isLambda = false)
            FirAnonymousFunctionBuilder().apply {
                source = functionDeclaration.toFirSourceElement()
                receiverTypeRef = receiverType
                symbol = FirAnonymousFunctionSymbol()
                isLambda = false
            }
        } else {
            val functionName = identifier.nameAsSafeName()
            val labelName = runIf(!functionName.isSpecial) { functionName.identifier }
            target = FirFunctionTarget(labelName, isLambda = false)
            FirSimpleFunctionBuilder().apply {
                source = functionDeclaration.toFirSourceElement()
                receiverTypeRef = receiverType
                name = functionName
                status = FirDeclarationStatusImpl(
                    if (isLocal) Visibilities.Local else modifiers.getVisibility(),
                    modifiers.getModality()
                ).apply {
                    isExpect = modifiers.hasExpect() || classWrapper?.hasExpect() == true
                    isActual = modifiers.hasActual()
                    isOverride = modifiers.hasOverride()
                    isOperator = modifiers.hasOperator()
                    isInfix = modifiers.hasInfix()
                    isInline = modifiers.hasInline()
                    isTailRec = modifiers.hasTailrec()
                    isExternal = modifiers.hasExternal()
                    isSuspend = modifiers.hasSuspend()
                }

                symbol = FirNamedFunctionSymbol(callableIdForName(functionName, isLocal))
            }
        }

        return functionBuilder.apply {
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = returnType!!

            context.firFunctionTargets += target
            annotations += modifiers.annotations

            withCapturedTypeParameters {
                if (this is FirSimpleFunctionBuilder) {
                    typeParameters += firTypeParameters
                    addCapturedTypeParameters(typeParameters)
                }
                valueParametersList?.let { list -> valueParameters += convertValueParameters(list).map { it.firValueParameter } }

                val hasContractEffectList = outerContractDescription != null
                val bodyWithContractDescription = convertFunctionBody(block, expression, hasContractEffectList)
                this.body = bodyWithContractDescription.first
                val contractDescription = outerContractDescription ?: bodyWithContractDescription.second
                contractDescription?.let {
                    // TODO: add error reporting for contracts on lambdas
                    if (this is FirSimpleFunctionBuilder) {
                        this.contractDescription = it
                    }
                }
            }
            context.firFunctionTargets.removeLast()
        }.build().also {
            target.bind(it)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionBody
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.buildFirBody
     */
    private fun convertFunctionBody(
        blockNode: LighterASTNode?,
        expression: LighterASTNode?,
        hasContractEffectList: Boolean = false
    ): Pair<FirBlock?, FirContractDescription?> {
        return when {
            blockNode != null -> {
                val block = convertBlock(blockNode)
                if (hasContractEffectList) {
                    block to null
                } else {
                    block.extractContractDescriptionIfPossible()
                }
            }
            expression != null -> FirSingleExpressionBlock(
                expressionConverter.getAsFirExpression<FirExpression>(expression, "Function has no body (but should)").toReturn()
            ) to null
            else -> null to null
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseBlock
     */
    fun convertBlock(block: LighterASTNode?): FirBlock {
        if (block == null) return buildEmptyExpressionBlock()
        if (block.tokenType != BLOCK) {
            return FirSingleExpressionBlock(
                expressionConverter.getAsFirExpression(block)
            )
        }
        return if (!stubMode) {
            val blockTree = LightTree2Fir.buildLightTreeBlockExpression(block.asText)
            return DeclarationsConverter(
                baseSession, baseScopeProvider, stubMode, blockTree, offset = tree.getStartOffset(block), context
            ).convertBlockExpression(blockTree.root)
        } else {
            val firExpression = buildExpressionStub()
            FirSingleExpressionBlock(
                firExpression.toReturn(baseSource = firExpression.source)
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseDelegationSpecifierList
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.extractSuperTypeListEntriesTo
     *
     * SUPER_TYPE_ENTRY             - userType
     * SUPER_TYPE_CALL_ENTRY        - constructorInvocation
     * DELEGATED_SUPER_TYPE_ENTRY   - explicitDelegation
     */
    //TODO make wrapper for result?
    private data class DelegationSpecifiers(
        val delegatedSuperTypeRef: FirTypeRef?,
        val superTypesRef: List<FirTypeRef>,
        val delegatedConstructorArguments: List<FirExpression>,
        val delegatedConstructorSource: FirLightSourceElement?,
        val delegateFields: List<FirField>,
        val primaryConstructorBody: FirBlock?
    )

    private fun convertDelegationSpecifiers(
        delegationSpecifiers: LighterASTNode,
        containerSymbol: AbstractFirBasedSymbol<*>,
        delegatedTypeRef: FirTypeRef
    ): DelegationSpecifiers {
        val superTypeRefs = mutableListOf<FirTypeRef>()
        val superTypeCallEntry = mutableListOf<FirExpression>()
        var delegatedSuperTypeRef: FirTypeRef? = null
        var delegateConstructorSource: FirLightSourceElement? = null
        val delegateFields = mutableListOf<FirField>()
        val initializeDelegateStatements = mutableListOf<FirStatement>()
        var delegateNumber = 0
        delegationSpecifiers.forEachChildren {
            when (it.tokenType) {
                SUPER_TYPE_ENTRY -> {
                    superTypeRefs += convertType(it)
                }
                SUPER_TYPE_CALL_ENTRY -> convertConstructorInvocation(it).apply {
                    delegatedSuperTypeRef = first
                    superTypeRefs += first
                    superTypeCallEntry += second
                    delegateConstructorSource = it.toFirSourceElement(FirFakeSourceElementKind.DelegatingConstructorCall)
                }
                DELEGATED_SUPER_TYPE_ENTRY -> {
                    superTypeRefs += convertExplicitDelegation(
                        it,
                        delegateNumber,
                        delegateFields,
                        initializeDelegateStatements,
                        containerSymbol,
                        delegatedTypeRef
                    )
                    delegateNumber++
                }
            }
        }
        val body = if (initializeDelegateStatements.isNotEmpty()) {
            buildBlock {
                for (statement in initializeDelegateStatements) {
                    statements += statement
                }
            }
        } else null
        return DelegationSpecifiers(
            delegatedSuperTypeRef, superTypeRefs, superTypeCallEntry, delegateConstructorSource,
            delegateFields, body
        )
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
                CONSTRUCTOR_CALLEE -> if (it.asText.isNotEmpty()) firTypeRef = convertType(it)   //is empty in enum entry constructor
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
        delegateNumber: Int,
        delegateFields: MutableList<FirField>,
        initializeDelegateStatements: MutableList<FirStatement>,
        containerSymbol: AbstractFirBasedSymbol<*>,
        delegatedSelfTypeRef: FirTypeRef
    ): FirTypeRef {
        lateinit var firTypeRef: FirTypeRef
        var firExpression: FirExpression? = buildErrorExpression(
            explicitDelegation.toFirSourceElement(), ConeSimpleDiagnostic("Should have delegate", DiagnosticKind.Syntax)
        )
        explicitDelegation.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firTypeRef = convertType(it)
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have delegate")
            }
        }

        val delegateName = Name.special("<\$\$delegate_$delegateNumber>")
        delegateFields.add(
            buildField {
                source = firExpression!!.source
                session = baseSession
                origin = FirDeclarationOrigin.Synthetic
                name = delegateName
                returnTypeRef = firTypeRef
                symbol = FirFieldSymbol(CallableId(name))
                isVar = false
                status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
            }
        )
        initializeDelegateStatements.add(
            buildVariableAssignment {
                source = firExpression!!.source
                calleeReference =
                    buildResolvedNamedReference {
                        name = delegateName
                        resolvedSymbol = delegateFields[delegateNumber].symbol
                    }
                rValue = firExpression!!
                dispatchReceiver = buildThisReceiverExpression {
                    calleeReference = buildImplicitThisReference {
                        boundSymbol = containerSymbol
                    }
                    typeRef = delegatedSelfTypeRef
                }
            }
        )
        return firTypeRef
    }

    /*****    TYPES    *****/
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeParameterList
     */
    private fun convertTypeParameters(typeParameterList: LighterASTNode, typeConstraints: List<TypeConstraint>): List<FirTypeParameter> {
        return typeParameterList.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                TYPE_PARAMETER -> container += convertTypeParameter(node, typeConstraints)
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
        lateinit var identifier: String
        lateinit var firType: FirTypeRef
        val annotations = mutableListOf<FirAnnotationCall>()
        typeConstraint.forEachChildren {
            when (it.tokenType) {
                //annotations will be saved later, on mapping stage with type parameters
                ANNOTATION, ANNOTATION_ENTRY -> annotations += convertAnnotation(it)
                REFERENCE_EXPRESSION -> identifier = it.asText
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        return TypeConstraint(annotations, identifier, firType)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeParameter
     */
    private fun convertTypeParameter(typeParameter: LighterASTNode, typeConstraints: List<TypeConstraint>): FirTypeParameter {
        var typeParameterModifiers = TypeParameterModifier()
        var identifier: String? = null
        var firType: FirTypeRef? = null
        typeParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> typeParameterModifiers = convertTypeParameterModifiers(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        return buildTypeParameter {
            source = typeParameter.toFirSourceElement()
            session = baseSession
            origin = FirDeclarationOrigin.Source
            name = identifier.nameAsSafeName()
            symbol = FirTypeParameterSymbol()
            variance = typeParameterModifiers.getVariance()
            isReified = typeParameterModifiers.hasReified()
            annotations += typeParameterModifiers.annotations
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
        if (type.asText.isEmpty()) {
            return buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Unwrapped type is null", DiagnosticKind.Syntax) }
        }
        var typeModifiers = TypeModifier()
        var firType: FirTypeRef = buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Incomplete code", DiagnosticKind.Syntax) }
        var afterLPar = false
        type.forEachChildren {
            when (it.tokenType) {
                LPAR -> afterLPar = true
                TYPE_REFERENCE -> firType = convertType(it)
                MODIFIER_LIST -> if (!afterLPar || typeModifiers.hasNoAnnotations()) typeModifiers = convertTypeModifierList(it)
                USER_TYPE -> firType = convertUserType(it)
                NULLABLE_TYPE -> firType = convertNullableType(it)
                FUNCTION_TYPE -> firType = convertFunctionType(it, isSuspend = typeModifiers.hasSuspend)
                DYNAMIC_TYPE -> firType = buildDynamicTypeRef {
                    source = type.toFirSourceElement()
                    isMarkedNullable = false
                }
                TokenType.ERROR_ELEMENT -> firType =
                    buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Unwrapped type is null", DiagnosticKind.Syntax) }
            }
        }

        return firType.also { (it.annotations as MutableList<FirAnnotationCall>) += typeModifiers.annotations }
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
    private fun convertNullableType(nullableType: LighterASTNode): FirTypeRef {
        lateinit var firType: FirTypeRef
        nullableType.forEachChildren {
            when (it.tokenType) {
                USER_TYPE -> firType =
                    convertUserType(it, true)
                FUNCTION_TYPE -> firType = convertFunctionType(it, true)
                NULLABLE_TYPE -> firType = convertNullableType(it)
                DYNAMIC_TYPE -> firType = buildDynamicTypeRef {
                    source = nullableType.toFirSourceElement()
                    isMarkedNullable = true
                }
            }
        }

        return firType
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseUserType
     */
    private fun convertUserType(userType: LighterASTNode, isNullable: Boolean = false): FirTypeRef {
        var simpleFirUserType: FirUserTypeRef? = null
        var identifier: String? = null
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        userType.forEachChildren {
            when (it.tokenType) {
                USER_TYPE -> simpleFirUserType = convertUserType(it) as? FirUserTypeRef //simple user type
                REFERENCE_EXPRESSION -> identifier = it.asText
                TYPE_ARGUMENT_LIST -> firTypeArguments += convertTypeArguments(it)
            }
        }

        if (identifier == null)
            return buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Incomplete user type", DiagnosticKind.Syntax) }

        val theSource = userType.toFirSourceElement()
        val qualifierPart = FirQualifierPartImpl(
            identifier.nameAsSafeName(),
            FirTypeArgumentListImpl(theSource).apply {
                typeArguments += firTypeArguments
            }
        )

        return buildUserTypeRef {
            source = theSource
            isMarkedNullable = isNullable
            qualifier.add(qualifierPart)
            simpleFirUserType?.qualifier?.let { this.qualifier.addAll(0, it) }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentList
     */
    fun convertTypeArguments(typeArguments: LighterASTNode): List<FirTypeProjection> {
        return typeArguments.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                TYPE_PROJECTION -> container += convertTypeProjection(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.tryParseTypeArgumentList
     */
    private fun convertTypeProjection(typeProjection: LighterASTNode): FirTypeProjection {
        var modifiers = TypeProjectionModifier()
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
        return if (isStarProjection) buildStarProjection { source = typeProjection.toFirSourceElement() }
        else buildTypeProjectionWithVariance {
            source = typeProjection.toFirSourceElement()
            typeRef = firType
            variance = modifiers.getVariance()
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionType
     */
    private fun convertFunctionType(functionType: LighterASTNode, isNullable: Boolean = false, isSuspend: Boolean = false): FirTypeRef {
        var receiverTypeReference: FirTypeRef? = null
        lateinit var returnTypeReference: FirTypeRef
        val valueParametersList = mutableListOf<ValueParameter>()
        functionType.forEachChildren {
            when (it.tokenType) {
                FUNCTION_TYPE_RECEIVER -> receiverTypeReference = convertReceiverType(it)
                VALUE_PARAMETER_LIST -> valueParametersList += convertValueParameters(it)
                TYPE_REFERENCE -> returnTypeReference = convertType(it)
            }
        }

        return buildFunctionTypeRef {
            source = functionType.toFirSourceElement()
            isMarkedNullable = isNullable
            receiverTypeRef = receiverTypeReference
            returnTypeRef = returnTypeReference
            valueParameters += valueParametersList.map { it.firValueParameter }
            if (receiverTypeReference != null) {
                annotations += extensionFunctionAnnotation
            }
            this.isSuspend = isSuspend
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameterList
     */
    fun convertValueParameters(valueParameters: LighterASTNode): List<ValueParameter> {
        return valueParameters.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_PARAMETER -> container += convertValueParameter(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameter
     */
    fun convertValueParameter(valueParameter: LighterASTNode): ValueParameter {
        var modifiers = Modifier()
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
                TYPE_REFERENCE -> firType = convertType(it)
                DESTRUCTURING_DECLARATION -> destructuringDeclaration = convertDestructingDeclaration(it)
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have default value")
            }
        }

        val name = identifier.nameAsSafeName()
        val firValueParameter = buildValueParameter {
            source = valueParameter.toFirSourceElement()
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = firType ?: implicitType
            this.name = name
            symbol = FirVariableSymbol(name)
            defaultValue = firExpression
            isCrossinline = modifiers.hasCrossinline()
            isNoinline = modifiers.hasNoinline()
            isVararg = modifiers.hasVararg()
            annotations += modifiers.annotations
        }
        return ValueParameter(isVal, isVar, modifiers, firValueParameter, destructuringDeclaration)
    }

    private val extensionFunctionAnnotation = buildAnnotationCall {
        annotationTypeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(ClassId.fromString(EXTENSION_FUNCTION_ANNOTATION)),
                emptyArray(),
                false
            )
        }
        calleeReference = FirReferencePlaceholderForResolvedAnnotations
    }
}
