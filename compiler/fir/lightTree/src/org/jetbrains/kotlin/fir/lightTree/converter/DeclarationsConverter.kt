/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate
import org.jetbrains.kotlin.fir.builder.generateComponentFunctions
import org.jetbrains.kotlin.fir.builder.generateCopyFunction
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.fir.*
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeModifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeParameterModifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeProjectionModifier
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class DeclarationsConverter(
    session: FirSession,
    val scopeProvider: FirScopeProvider,
    private val stubMode: Boolean,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    context: Context = Context()
) : BaseConverter(session, tree, context) {
    private val expressionConverter = ExpressionsConverter(session, stubMode, tree, this, context)

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

        val firFile = FirFileImpl(
            null,
            session,
            fileName,
            context.packageFqName
        )
        firFile.annotations += fileAnnotationList
        firFile.imports += importList
        firFile.declarations += firDeclarationList

        return firFile
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseBlockExpression
     */
    fun convertBlockExpression(block: LighterASTNode): FirBlock {
        val firStatements = block.forEachChildrenReturnList<FirStatement> { node, container ->
            when (node.tokenType) {
                CLASS, OBJECT_DECLARATION -> container += convertClass(node) as FirStatement
                FUN -> container += convertFunctionDeclaration(node) as FirStatement
                PROPERTY -> container += convertPropertyDeclaration(node) as FirStatement
                DESTRUCTURING_DECLARATION -> container += convertDestructingDeclaration(node).toFirDestructingDeclaration(session)
                TYPEALIAS -> container += convertTypeAlias(node) as FirStatement
                CLASS_INITIALIZER -> container += convertAnonymousInitializer(node) as FirStatement
                else -> if (node.isExpression()) container += expressionConverter.getAsFirExpression<FirStatement>(node)
            }
        }
        return FirBlockImpl(null).apply {
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

        return FirImportImpl(
            null,
            importedFqName,
            isAllUnder,
            aliasName?.let { Name.identifier(it) }
        )
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
    private fun convertModifierList(modifiers: LighterASTNode): Modifier {
        val modifier = Modifier()
        modifiers.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> modifier.annotations += convertAnnotation(it)
                ANNOTATION_ENTRY -> modifier.annotations += convertAnnotationEntry(it)
                is KtModifierKeywordToken -> modifier.addModifier(it)
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
        return FirAnnotationCallImpl(
            null,
            annotationUseSiteTarget ?: defaultAnnotationUseSiteTarget,
            constructorCalleePair.first
        ).extractArgumentsFrom(constructorCalleePair.second, stubMode)
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
        val superTypeRefs = mutableListOf<FirTypeRef>()
        val superTypeCallEntry = mutableListOf<FirExpression>()
        var delegatedSuperTypeRef: FirTypeRef? = null
        val typeConstraints = mutableListOf<TypeConstraint>()
        var classBody: LighterASTNode? = null
        classNode.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                CLASS_KEYWORD -> classKind = ClassKind.CLASS
                INTERFACE_KEYWORD -> classKind = ClassKind.INTERFACE
                OBJECT_KEYWORD -> classKind = ClassKind.OBJECT
                IDENTIFIER -> identifier = it.asText
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                PRIMARY_CONSTRUCTOR -> primaryConstructor = it
                SUPER_TYPE_LIST -> convertDelegationSpecifiers(it).apply {
                    delegatedSuperTypeRef = first
                    superTypeRefs += second
                    superTypeCallEntry += third
                }
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                CLASS_BODY -> classBody = it
            }
        }



        if (classKind == ClassKind.CLASS) {
            classKind = when {
                modifiers.isEnum() -> ClassKind.ENUM_CLASS
                modifiers.isAnnotation() -> ClassKind.ANNOTATION_CLASS
                else -> classKind
            }
        }

        val className = identifier.nameAsSafeName(if (modifiers.isCompanion()) "Companion" else "")
        val isLocal = isClassLocal(classNode) { getParent() }

        return withChildClassName(className) {
            val status = FirDeclarationStatusImpl(
                if (isLocal) Visibilities.LOCAL else modifiers.getVisibility(),
                modifiers.getModality()
            ).apply {
                isExpect = modifiers.hasExpect()
                isActual = modifiers.hasActual()
                isInner = modifiers.isInner()
                isCompanion = modifiers.isCompanion() && classKind == ClassKind.OBJECT
                isData = modifiers.isDataClass() && classKind != ClassKind.OBJECT
                isInline = modifiers.hasInline()
            }
            val firClass = if (status.modality == Modality.SEALED) {
                FirSealedClassImpl(
                    null,
                    session,
                    className,
                    status,
                    classKind,
                    scopeProvider,
                    FirRegularClassSymbol(context.currentClassId)
                )
            } else {
                FirClassImpl(
                    null,
                    session,
                    className,
                    status,
                    classKind,
                    scopeProvider,
                    FirRegularClassSymbol(context.currentClassId)
                )
            }
            firClass.annotations += modifiers.annotations
            firClass.typeParameters += firTypeParameters
            firClass.joinTypeParameters(typeConstraints)

            val selfType = null.toDelegatedSelfType(firClass)

            when {
                modifiers.isEnum() && (classKind == ClassKind.ENUM_CLASS) -> {
                    superTypeRefs += FirResolvedTypeRefImpl(
                        source = null,
                        ConeClassLikeTypeImpl(
                            implicitEnumType.type.lookupTag,
                            arrayOf(selfType.coneTypeUnsafe()),
                            isNullable = false
                        )
                    )
                }
                modifiers.isAnnotation() && (classKind == ClassKind.ANNOTATION_CLASS) -> {
                    superTypeRefs += implicitAnnotationType
                }
            }
            val defaultDelegatedSuperTypeRef = implicitAnyType

            superTypeRefs.ifEmpty { superTypeRefs += defaultDelegatedSuperTypeRef }

            firClass.superTypeRefs += superTypeRefs

            val classWrapper = ClassWrapper(
                className, modifiers, classKind, primaryConstructor != null,
                classBody.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                selfType,
                delegatedSuperTypeRef ?: defaultDelegatedSuperTypeRef, superTypeCallEntry
            )
            //parse primary constructor
            val primaryConstructorWrapper = convertPrimaryConstructor(primaryConstructor, classWrapper)
            val firPrimaryConstructor = primaryConstructorWrapper?.firConstructor
            firPrimaryConstructor?.let { firClass.declarations += it }

            val properties = mutableListOf<FirProperty>()
            if (primaryConstructor != null && firPrimaryConstructor != null) {
                //parse properties
                properties += primaryConstructorWrapper.valueParameters
                    .filter { it.hasValOrVar() }
                    .map { it.toFirProperty(session, callableIdForName(it.firValueParameter.name)) }
                firClass.addDeclarations(properties)
            }

            //parse declarations
            classBody?.let {
                firClass.addDeclarations(convertClassBody(it, classWrapper))
            }

            //parse data class
            if (modifiers.isDataClass() && firPrimaryConstructor != null) {
                val zippedParameters = MutableList(properties.size) { null }.zip(properties)
                zippedParameters.generateComponentFunctions(
                    session, firClass, context.packageFqName, context.className, firPrimaryConstructor
                )
                zippedParameters.generateCopyFunction(
                    session, null, firClass, context.packageFqName, context.className, firPrimaryConstructor
                )
                // TODO: equals, hashCode, toString
            }

            if (modifiers.isEnum()) {
                firClass.generateValuesFunction(session, context.packageFqName, context.className)
                firClass.generateValueOfFunction(session, context.packageFqName, context.className)
            }

            return@withChildClassName firClass
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseObjectLiteral
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitObjectLiteralExpression
     */
    fun convertObjectLiteral(objectLiteral: LighterASTNode): FirElement {
        var modifiers = Modifier()
        var primaryConstructor: LighterASTNode? = null
        val superTypeRefs = mutableListOf<FirTypeRef>()
        val superTypeCallEntry = mutableListOf<FirExpression>()
        var delegatedSuperTypeRef: FirTypeRef? = null
        var classBody: LighterASTNode? = null
        objectLiteral.getChildNodesByType(OBJECT_DECLARATION).first().forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                PRIMARY_CONSTRUCTOR -> primaryConstructor = it
                SUPER_TYPE_LIST -> convertDelegationSpecifiers(it).apply {
                    delegatedSuperTypeRef = first
                    superTypeRefs += second
                    superTypeCallEntry += third
                }
                CLASS_BODY -> classBody = it
            }
        }

        superTypeRefs.ifEmpty { superTypeRefs += implicitAnyType }
        val delegatedType = delegatedSuperTypeRef ?: implicitAnyType

        return withChildClassName(ANONYMOUS_OBJECT_NAME) {
            FirAnonymousObjectImpl(null, session, ClassKind.OBJECT, scopeProvider, FirAnonymousObjectSymbol()).apply {
                annotations += modifiers.annotations
                this.superTypeRefs += superTypeRefs
                this.typeRef = superTypeRefs.first()

                val classWrapper = ClassWrapper(
                    SpecialNames.NO_NAME_PROVIDED, modifiers, ClassKind.OBJECT, hasPrimaryConstructor = false,
                    hasSecondaryConstructor = classBody.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                    delegatedSelfTypeRef = delegatedType,
                    delegatedSuperTypeRef = delegatedType,
                    superTypeCallEntry = superTypeCallEntry
                )
                //parse primary constructor
                convertPrimaryConstructor(primaryConstructor, classWrapper)?.let { this.declarations += it.firConstructor }

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
        var hasInitializerList = false
        val enumSuperTypeCallEntry = mutableListOf<FirExpression>()
        var classBodyNode: LighterASTNode? = null
        enumEntry.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                INITIALIZER_LIST -> {
                    hasInitializerList = true
                    enumSuperTypeCallEntry += convertInitializerList(it)
                }
                CLASS_BODY -> classBodyNode = it
            }
        }

        val enumEntryName = identifier.nameAsSafeName()
        return FirEnumEntryImpl(
            null,
            session,
            classWrapper.delegatedSelfTypeRef,
            name = enumEntryName,
            initializer = FirAnonymousObjectImpl(
                null,
                session,
                ClassKind.ENUM_ENTRY,
                scopeProvider,
                FirAnonymousObjectSymbol()
            ).apply {
                annotations += modifiers.annotations
                val enumClassWrapper = ClassWrapper(
                    enumEntryName, modifiers, ClassKind.ENUM_ENTRY, hasPrimaryConstructor = true,
                    hasSecondaryConstructor = classBodyNode.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                    delegatedSelfTypeRef = FirResolvedTypeRefImpl(
                        null,
                        ConeClassLikeTypeImpl(
                            symbol.toLookupTag(),
                            emptyArray(),
                            isNullable = false
                        )
                    ),
                    delegatedSuperTypeRef = classWrapper.delegatedSelfTypeRef,
                    superTypeCallEntry = enumSuperTypeCallEntry
                )
                superTypeRefs += enumClassWrapper.delegatedSuperTypeRef
                convertPrimaryConstructor(null, enumClassWrapper)?.let { declarations += it.firConstructor }
                classBodyNode?.also { declarations += convertClassBody(it, enumClassWrapper) }
            },
            symbol = FirVariableSymbol(CallableId(context.currentClassId, enumEntryName)),
            status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
                isStatic = true
            }
        )
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
                FUN -> container += convertFunctionDeclaration(node)
                PROPERTY -> container += convertPropertyDeclaration(node)
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
    private fun convertPrimaryConstructor(primaryConstructor: LighterASTNode?, classWrapper: ClassWrapper): PrimaryConstructor? {
        if (primaryConstructor == null && classWrapper.hasSecondaryConstructor) return null
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
        val firDelegatedCall = FirDelegatedConstructorCallImpl(
            null,
            classWrapper.delegatedSuperTypeRef,
            isThis = false
        ).extractArgumentsFrom(classWrapper.superTypeCallEntry, stubMode)

        val status = FirDeclarationStatusImpl(
            if (primaryConstructor != null) modifiers.getVisibility() else defaultVisibility,
            Modality.FINAL
        ).apply {
            isExpect = modifiers.hasExpect()
            isActual = modifiers.hasActual()
            isInner = classWrapper.isInner()
        }

        return PrimaryConstructor(
            FirPrimaryConstructorImpl(
                null,
                session,
                classWrapper.delegatedSelfTypeRef,
                null,
                status,
                FirConstructorSymbol(callableIdForClassConstructor())
            ).apply {
                annotations += modifiers.annotations
                this.typeParameters += typeParametersFromSelfType(classWrapper.delegatedSelfTypeRef)
                this.valueParameters += valueParameters.map { it.firValueParameter }
                this.delegatedConstructor = firDelegatedCall
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

        return FirAnonymousInitializerImpl(
            null,
            session,
            if (stubMode) FirEmptyExpressionBlock() else firBlock
        )
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

        val delegatedSelfTypeRef =
            if (classWrapper.isObjectLiteral()) FirErrorTypeRefImpl(
                null,
                FirSimpleDiagnostic(
                    "Constructor in object",
                    DiagnosticKind.ConstructorInObject
                )
            )
            else classWrapper.delegatedSelfTypeRef

        val status = FirDeclarationStatusImpl(modifiers.getVisibility(), Modality.FINAL).apply {
            isExpect = modifiers.hasExpect()
            isActual = modifiers.hasActual()
            isInner = classWrapper.isInner()
        }

        val firConstructor = FirConstructorImpl(
            null,
            session,
            delegatedSelfTypeRef,
            null,
            status,
            FirConstructorSymbol(callableIdForClassConstructor())
        ).apply {
            delegatedConstructor = constructorDelegationCall
        }

        context.firFunctions += firConstructor
        firConstructor.annotations += modifiers.annotations
        firConstructor.typeParameters += typeParametersFromSelfType(delegatedSelfTypeRef)
        firConstructor.valueParameters += firValueParameters.map { it.firValueParameter }
        firConstructor.body = convertFunctionBody(block, null)
        context.firFunctions.removeLast()
        return firConstructor
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.convert(
     * KtConstructorDelegationCall, FirTypeRef, Boolean)
     */
    private fun convertConstructorDelegationCall(
        constructorDelegationCall: LighterASTNode,
        classWrapper: ClassWrapper
    ): FirDelegatedConstructorCallImpl {
        var thisKeywordPresent = false
        val firValueArguments = mutableListOf<FirExpression>()
        constructorDelegationCall.forEachChildren {
            when (it.tokenType) {
                CONSTRUCTOR_DELEGATION_REFERENCE -> if (it.asText == "this") thisKeywordPresent = true
                VALUE_ARGUMENT_LIST -> firValueArguments += expressionConverter.convertValueArguments(it)
            }
        }

        val isImplicit = constructorDelegationCall.asText.isEmpty()
        val isThis = (isImplicit && classWrapper.hasPrimaryConstructor) || thisKeywordPresent
        val delegatedType =
            if (classWrapper.isObjectLiteral() || classWrapper.isInterface()) when {
                isThis -> FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Constructor in object", DiagnosticKind.ConstructorInObject))
                else -> FirErrorTypeRefImpl(null, FirSimpleDiagnostic("No super type", DiagnosticKind.Syntax))
            }
            else when {
                isThis -> classWrapper.delegatedSelfTypeRef
                else -> classWrapper.delegatedSuperTypeRef
            }

        return FirDelegatedConstructorCallImpl(
            null,
            delegatedType,
            isThis
        ).extractArgumentsFrom(firValueArguments, stubMode)
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
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val typeAliasName = identifier.nameAsSafeName()
        val status = FirDeclarationStatusImpl(modifiers.getVisibility(), Modality.FINAL).apply {
            isExpect = modifiers.hasExpect()
            isActual = modifiers.hasActual()
        }
        return withChildClassName(typeAliasName) {
            return@withChildClassName FirTypeAliasImpl(
                null,
                session,
                typeAliasName,
                status,
                FirTypeAliasSymbol(context.currentClassId),
                firType
            ).apply {
                annotations += modifiers.annotations
                typeParameters += firTypeParameters
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseProperty
     */
    fun convertPropertyDeclaration(property: LighterASTNode): FirDeclaration {
        var modifiers = Modifier()
        var identifier: String? = null
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        var isReturnType = false
        var delegateExpression: LighterASTNode? = null
        var isVar = false
        var receiverType: FirTypeRef? = null
        var returnType: FirTypeRef = implicitType
        val typeConstraints = mutableListOf<TypeConstraint>()
        var getter: FirPropertyAccessor? = null
        var setter: FirPropertyAccessor? = null
        var firExpression: FirExpression? = null
        property.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                COLON -> isReturnType = true
                TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverType = convertType(it)
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                PROPERTY_DELEGATE -> delegateExpression = it
                VAR_KEYWORD -> isVar = true
                PROPERTY_ACCESSOR -> {
                    val propertyAccessor = convertGetterOrSetter(it, returnType)
                    if (propertyAccessor.isGetter) getter = propertyAccessor else setter = propertyAccessor
                }
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have initializer")
            }
        }

        val propertyName = identifier.nameAsSafeName()

        val parentNode = property.getParent()
        val isLocal = !(parentNode?.tokenType == KT_FILE || parentNode?.tokenType == CLASS_BODY)
        return if (isLocal) {
            val receiver =
                delegateExpression?.let { expressionConverter.getAsFirExpression<FirExpression>(it, "Incorrect delegate expression") }
            FirPropertyImpl(
                null,
                session,
                returnType,
                null,
                propertyName,
                firExpression,
                delegateExpression?.let {
                    FirWrappedDelegateExpressionImpl(
                        null, expressionConverter.getAsFirExpression(it, "Incorrect delegate expression")
                    )
                },
                isVar,
                FirPropertySymbol(CallableId(propertyName)),
                true,
                FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
            ).apply {
                annotations += modifiers.annotations
                this.generateAccessorsByDelegate(this@DeclarationsConverter.session, member = false, stubMode, receiver)
            }
        } else {
            val status = FirDeclarationStatusImpl(modifiers.getVisibility(), modifiers.getModality()).apply {
                isExpect = modifiers.hasExpect()
                isActual = modifiers.hasActual()
                isOverride = modifiers.hasOverride()
                isConst = modifiers.isConst()
                isLateInit = modifiers.hasLateinit()
            }
            val receiver = delegateExpression?.let {
                expressionConverter.getAsFirExpression<FirExpression>(it, "Should have delegate")
            }
            FirPropertyImpl(
                null,
                session,
                returnType,
                receiverType,
                propertyName,
                firExpression,
                delegateExpression?.let {
                    FirWrappedDelegateExpressionImpl(
                        null,
                        expressionConverter.getAsFirExpression(it, "Should have delegate")
                    )
                },
                isVar,
                FirPropertySymbol(callableIdForName(propertyName)),
                false,
                status
            ).apply {
                this.typeParameters += firTypeParameters
                this.joinTypeParameters(typeConstraints)
                annotations += modifiers.annotations
                this.getter = getter ?: FirDefaultPropertyGetter(null, session, returnType, modifiers.getVisibility())
                this.setter = if (isVar) setter ?: FirDefaultPropertySetter(null, session, returnType, modifiers.getVisibility()) else null
                generateAccessorsByDelegate(
                    this@DeclarationsConverter.session, member = parentNode?.tokenType != KT_FILE, stubMode, receiver
                )
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitDestructuringDeclaration
     */
    private fun convertDestructingDeclaration(destructingDeclaration: LighterASTNode): DestructuringDeclaration {
        var isVar = false
        val entries = mutableListOf<FirVariable<*>>()
        var firExpression: FirExpression =
            FirErrorExpressionImpl(null, FirSimpleDiagnostic("Destructuring declaration without initializer", DiagnosticKind.Syntax))
        destructingDeclaration.forEachChildren {
            when (it.tokenType) {
                VAR_KEYWORD -> isVar = true
                DESTRUCTURING_DECLARATION_ENTRY -> entries += convertDestructingDeclarationEntry(it)
                else -> if (it.isExpression()) firExpression =
                    expressionConverter.getAsFirExpression(it, "Destructuring declaration without initializer")
            }
        }

        return DestructuringDeclaration(isVar, entries, firExpression)
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
        return FirPropertyImpl(
            null,
            session,
            firType ?: implicitType,
            null,
            name,
            null,
            null,
            false,
            FirPropertySymbol(CallableId(name)),
            true,
            FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
        ).apply {
            annotations += modifiers.annotations
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyGetterOrSetter
     */
    private fun convertGetterOrSetter(getterOrSetter: LighterASTNode, propertyTypeRef: FirTypeRef): FirPropertyAccessor {
        var modifiers = Modifier()
        var isGetter = true
        var returnType: FirTypeRef? = null
        var firValueParameters: FirValueParameter = FirDefaultSetterValueParameter(
            null,
            session,
            propertyTypeRef,
            FirVariableSymbol(NAME_FOR_DEFAULT_VALUE_PARAMETER)
        )
        var block: LighterASTNode? = null
        var expression: LighterASTNode? = null
        getterOrSetter.forEachChildren {
            if (it.asText == "set") isGetter = false
            when (it.tokenType) {
                SET_KEYWORD -> isGetter = false
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                TYPE_REFERENCE -> returnType = convertType(it)
                VALUE_PARAMETER_LIST -> firValueParameters = convertSetterParameter(it, propertyTypeRef)
                BLOCK -> block = it
                else -> if (it.isExpression()) expression = it
            }
        }

        val status = FirDeclarationStatusImpl(modifiers.getVisibility(), Modality.FINAL)

        val firAccessor = FirPropertyAccessorImpl(
            null,
            session,
            returnType ?: if (isGetter) propertyTypeRef else implicitUnitType,
            FirPropertyAccessorSymbol(),
            isGetter,
            status
        )
        context.firFunctions += firAccessor
        firAccessor.annotations += modifiers.annotations

        if (!isGetter) {
            firAccessor.valueParameters += firValueParameters
        }

        firAccessor.body = convertFunctionBody(block, expression)
        context.firFunctions.removeLast()
        return firAccessor
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

        return FirValueParameterImpl(
            null,
            session,
            if (firValueParameter.returnTypeRef == implicitType) propertyTypeRef else firValueParameter.returnTypeRef,
            firValueParameter.name,
            FirVariableSymbol(firValueParameter.name),
            firValueParameter.defaultValue,
            isCrossinline = modifiers.hasCrossinline() || firValueParameter.isCrossinline,
            isNoinline = modifiers.hasNoinline() || firValueParameter.isNoinline,
            isVararg = modifiers.hasVararg() || firValueParameter.isVararg
        ).apply {
            annotations += modifiers.annotations + firValueParameter.annotations
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunction
     */
    fun convertFunctionDeclaration(functionDeclaration: LighterASTNode): FirDeclaration {
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
        functionDeclaration.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.asText
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                VALUE_PARAMETER_LIST -> valueParametersList = it //must convert later, because it can contains "return"
                COLON -> isReturnType = true
                TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverType = convertType(it)
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                BLOCK -> block = it
                EQ -> hasEqToken = true
                else -> if (it.isExpression()) expression = it
            }
        }

        if (returnType == null) {
            returnType =
                if (block != null || !hasEqToken) implicitUnitType
                else implicitType
        }

        val parentNode = functionDeclaration.getParent()
        val isLocal = !(parentNode?.tokenType == KT_FILE || parentNode?.tokenType == CLASS_BODY)
        val firFunction = if (identifier == null) {
            FirAnonymousFunctionImpl(null, session, returnType!!, receiverType, FirAnonymousFunctionSymbol(), isLambda = false)
        } else {
            val functionName = identifier.nameAsSafeName()
            val status = FirDeclarationStatusImpl(
                if (isLocal) Visibilities.LOCAL else modifiers.getVisibility(),
                modifiers.getModality()
            ).apply {
                isExpect = modifiers.hasExpect()
                isActual = modifiers.hasActual()
                isOverride = modifiers.hasOverride()
                isOperator = modifiers.hasOperator()
                isInfix = modifiers.hasInfix()
                isInline = modifiers.hasInline()
                isTailRec = modifiers.hasTailrec()
                isExternal = modifiers.hasExternal()
                isSuspend = modifiers.hasSuspend()
            }
            FirSimpleFunctionImpl(
                null,
                session,
                returnType!!,
                receiverType,
                functionName,
                status,
                FirNamedFunctionSymbol(callableIdForName(functionName, isLocal))
            )
        }

        context.firFunctions += firFunction
        firFunction.annotations += modifiers.annotations

        if (firFunction is FirSimpleFunctionImpl) {
            firFunction.typeParameters += firTypeParameters
            firFunction.joinTypeParameters(typeConstraints)
        }

        valueParametersList?.let { firFunction.valueParameters += convertValueParameters(it).map { it.firValueParameter } }
        firFunction.body = convertFunctionBody(block, expression)
        context.firFunctions.removeLast()
        return firFunction
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionBody
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.buildFirBody
     */
    private fun convertFunctionBody(blockNode: LighterASTNode?, expression: LighterASTNode?): FirBlock? {
        return when {
            blockNode != null -> return convertBlock(blockNode)
            expression != null -> FirSingleExpressionBlock(
                expressionConverter.getAsFirExpression<FirExpression>(expression, "Function has no body (but should)").toReturn()
            )
            else -> null
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseBlock
     */
    fun convertBlock(block: LighterASTNode?): FirBlock {
        if (block == null) return FirEmptyExpressionBlock()
        if (block.tokenType != BLOCK) {
            return FirSingleExpressionBlock(
                expressionConverter.getAsFirExpression(block)
            )
        }
        return if (!stubMode) {
            val blockTree = LightTree2Fir.buildLightTreeBlockExpression(block.asText)
            return DeclarationsConverter(session, scopeProvider, stubMode, blockTree, context).convertBlockExpression(blockTree.root)
        } else {
            FirSingleExpressionBlock(
                FirExpressionStub(null).toReturn()
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
    private fun convertDelegationSpecifiers(delegationSpecifiers: LighterASTNode): Triple<FirTypeRef?, List<FirTypeRef>, List<FirExpression>> {
        val superTypeRefs = mutableListOf<FirTypeRef>()
        val superTypeCallEntry = mutableListOf<FirExpression>()
        var delegatedSuperTypeRef: FirTypeRef? = null
        delegationSpecifiers.forEachChildren {
            when (it.tokenType) {
                SUPER_TYPE_ENTRY -> superTypeRefs += convertType(it)
                SUPER_TYPE_CALL_ENTRY -> convertConstructorInvocation(it).apply {
                    delegatedSuperTypeRef = first
                    superTypeRefs += first
                    superTypeCallEntry += second
                }
                DELEGATED_SUPER_TYPE_ENTRY -> superTypeRefs += convertExplicitDelegation(it)
            }
        }
        return Triple(delegatedSuperTypeRef, superTypeRefs, superTypeCallEntry)
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
    private fun convertExplicitDelegation(explicitDelegation: LighterASTNode): FirDelegatedTypeRef {
        lateinit var firTypeRef: FirTypeRef
        var firExpression: FirExpression? = FirErrorExpressionImpl(null, FirSimpleDiagnostic("Should have delegate", DiagnosticKind.Syntax))
        explicitDelegation.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firTypeRef = convertType(it)
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have delegate")
            }
        }

        return FirDelegatedTypeRefImpl(
            firExpression,
            firTypeRef
        )
    }

    /*****    TYPES    *****/
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeParameterList
     */
    private fun convertTypeParameters(typeParameterList: LighterASTNode): List<FirTypeParameter> {
        return typeParameterList.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                TYPE_PARAMETER -> container += convertTypeParameter(node)
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
    private fun convertTypeParameter(typeParameter: LighterASTNode): FirTypeParameter {
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

        val firTypeParameter = FirTypeParameterImpl(
            null,
            session,
            identifier.nameAsSafeName(),
            FirTypeParameterSymbol(),
            typeParameterModifiers.getVariance(),
            typeParameterModifiers.hasReified()
        )
        firTypeParameter.annotations += typeParameterModifiers.annotations
        firType?.let { firTypeParameter.bounds += it }

        firTypeParameter.addDefaultBoundIfNecessary()

        return firTypeParameter
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeRef
     */
    fun convertType(type: LighterASTNode): FirTypeRef {
        if (type.asText.isEmpty()) {
            return FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Unwrapped type is null", DiagnosticKind.Syntax))
        }
        var typeModifiers = TypeModifier() //TODO what with suspend?
        var firType: FirTypeRef = FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Incomplete code", DiagnosticKind.Syntax))
        var afterLPar = false
        type.forEachChildren {
            when (it.tokenType) {
                LPAR -> afterLPar = true
                TYPE_REFERENCE -> firType = convertType(it)
                MODIFIER_LIST -> if (!afterLPar || typeModifiers.hasNoAnnotations()) typeModifiers = convertTypeModifierList(it)
                USER_TYPE -> firType = convertUserType(it)
                NULLABLE_TYPE -> firType = convertNullableType(it)
                FUNCTION_TYPE -> firType = convertFunctionType(it)
                DYNAMIC_TYPE -> firType = FirDynamicTypeRefImpl(null, false)
                TokenType.ERROR_ELEMENT -> firType =
                    FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Unwrapped type is null", DiagnosticKind.Syntax))
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
                DYNAMIC_TYPE -> firType = FirDynamicTypeRefImpl(null, true)
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
            return FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Incomplete user type", DiagnosticKind.Syntax))

        val qualifier = FirQualifierPartImpl(
            identifier.nameAsSafeName()
        ).apply { typeArguments += firTypeArguments }

        return FirUserTypeRefImpl(
            null,
            isNullable
        ).apply {
            this.qualifier.add(qualifier)
            simpleFirUserType?.qualifier?.let { this@apply.qualifier.addAll(0, it) }
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
        return if (isStarProjection) FirStarProjectionImpl(null)
        else FirTypeProjectionWithVarianceImpl(
            null,
            firType,
            modifiers.getVariance()
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionType
     */
    private fun convertFunctionType(functionType: LighterASTNode, isNullable: Boolean = false): FirTypeRef {
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

        return FirFunctionTypeRefImpl(
            null,
            isNullable,
            receiverTypeReference,
            returnTypeReference
        ).apply {
            valueParameters += valueParametersList.map { it.firValueParameter }
            if (receiverTypeReference != null) {
                annotations += extensionFunctionAnnotation
            }
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
        val firValueParameter = FirValueParameterImpl(
            null,
            session,
            firType ?: implicitType,
            name,
            FirVariableSymbol(name),
            firExpression,
            isCrossinline = modifiers.hasCrossinline(),
            isNoinline = modifiers.hasNoinline(),
            isVararg = modifiers.hasVararg()
        ).apply { annotations += modifiers.annotations }
        return ValueParameter(isVal, isVar, modifiers, firValueParameter, destructuringDeclaration)
    }

    private val extensionFunctionAnnotation = FirAnnotationCallImpl(
        null,
        null,
        FirResolvedTypeRefImpl(
            null,
            ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(ClassId.fromString(EXTENSION_FUNCTION_ANNOTATION)),
                emptyArray(),
                false
            )
        )
    )
}
