/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.extractArgumentsFrom
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.getAsString
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.getAsStringWithoutBacktick
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.isExpression
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.joinTypeParameters
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.nameAsSafeName
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.toDelegatedSelfType
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.toReturn
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.isClassLocal
import org.jetbrains.kotlin.fir.lightTree.converter.DataClassUtil.generateComponentFunctions
import org.jetbrains.kotlin.fir.lightTree.converter.DataClassUtil.generateCopyFunction
import org.jetbrains.kotlin.fir.lightTree.converter.FunctionUtil.removeLast
import org.jetbrains.kotlin.fir.lightTree.fir.ClassWrapper
import org.jetbrains.kotlin.fir.lightTree.fir.DestructuringDeclaration
import org.jetbrains.kotlin.fir.lightTree.fir.TypeConstraint
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeModifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeParameterModifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.TypeProjectionModifier
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirDelegatedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class DeclarationsConverter(
    private val session: FirSession,
    private val stubMode: Boolean,
    tree: FlyweightCapableTreeStructure<LighterASTNode>
) : BaseConverter(session, tree) {
    private val expressionConverter = ExpressionsConverter(session, stubMode, tree, this)

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
        ClassNameUtil.packageFqName = FqName.ROOT
        file.forEachChildren {
            when (it.tokenType) {
                FILE_ANNOTATION_LIST -> fileAnnotationList += convertFileAnnotationList(it)
                PACKAGE_DIRECTIVE -> ClassNameUtil.packageFqName = convertPackageName(it)
                IMPORT_LIST -> importList += convertImportDirectives(it)
                CLASS -> firDeclarationList += convertClass(it)
                FUN -> firDeclarationList += convertFunctionDeclaration(it)
                PROPERTY -> firDeclarationList += convertPropertyDeclaration(it)
                TYPEALIAS -> firDeclarationList += convertTypeAlias(it)
                OBJECT_DECLARATION -> firDeclarationList += convertClass(it)
            }
        }

        val firFile = FirFileImpl(
            session,
            null,
            fileName,
            ClassNameUtil.packageFqName
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
        val firStatements = mutableListOf<FirStatement>()
        block.forEachChildren {
            when (it.tokenType) {
                CLASS -> firStatements += convertClass(it) as FirStatement
                FUN -> firStatements += convertFunctionDeclaration(it) as FirStatement
                PROPERTY -> firStatements += convertPropertyDeclaration(it) as FirStatement
                DESTRUCTURING_DECLARATION -> firStatements += convertDestructingDeclaration(it).toFirDestructingDeclaration(session)
                TYPEALIAS -> firStatements += convertTypeAlias(it) as FirStatement
                OBJECT_DECLARATION -> firStatements += convertClass(it) as FirStatement
                CLASS_INITIALIZER -> firStatements += convertAnonymousInitializer(it) as FirStatement
                else -> if (it.isExpression()) firStatements += expressionConverter.getAsFirExpression<FirStatement>(it)
            }
        }
        return FirBlockImpl(session, null).apply {
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
                IDENTIFIER -> return it.getAsString()
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
                DOT_QUALIFIED_EXPRESSION, REFERENCE_EXPRESSION -> {
                    var importName = it.getAsString()
                    if (importName.endsWith(".*")) {
                        isAllUnder = true
                        importName = importName.replace(".*", "")
                    }
                    importedFqName = FqName(importName)
                }
                IMPORT_ALIAS -> aliasName = convertImportAlias(it)
            }
        }

        return FirImportImpl(
            session,
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
        val modifier = Modifier(session)
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
        val typeModifierList = TypeModifier(session)
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
        val typeArgumentModifierList = TypeProjectionModifier(session)
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
        val modifier = TypeParameterModifier(session)
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
            session,
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
        var modifiers = Modifier(session)
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
                IDENTIFIER -> identifier = it.getAsString()
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

        val defaultDelegatedSuperTypeRef = when {
            modifiers.isEnum() && (classKind == ClassKind.CLASS || classKind == ClassKind.INTERFACE) -> implicitEnumType
            modifiers.isAnnotation() && (classKind == ClassKind.CLASS || classKind == ClassKind.INTERFACE) -> implicitAnnotationType
            else -> implicitAnyType
        }

        if (classKind == ClassKind.CLASS) {
            classKind = when {
                modifiers.isEnum() -> ClassKind.ENUM_CLASS
                modifiers.isAnnotation() -> ClassKind.ANNOTATION_CLASS
                else -> classKind
            }
        }

        val className = identifier.nameAsSafeName(if (modifiers.isCompanion()) "Companion" else "")
        superTypeRefs.ifEmpty { superTypeRefs += defaultDelegatedSuperTypeRef }
        val isLocal = isClassLocal(classNode) { getParent() }

        return ClassNameUtil.withChildClassName(className) {
            val firClass = FirClassImpl(
                session,
                null,
                FirClassSymbol(ClassNameUtil.currentClassId),
                className,
                if (isLocal) Visibilities.LOCAL else modifiers.getVisibility(),
                modifiers.getModality(),
                modifiers.hasExpect(),
                modifiers.hasActual(),
                classKind,
                isInner = modifiers.isInner(),
                isCompanion = modifiers.isCompanion() && classKind == ClassKind.OBJECT,
                isData = modifiers.isDataClass() && classKind != ClassKind.OBJECT,
                isInline = modifiers.hasInline()
            )
            firClass.annotations += modifiers.annotations
            firClass.typeParameters += firTypeParameters
            firClass.joinTypeParameters(typeConstraints)
            firClass.superTypeRefs += superTypeRefs

            val classWrapper = ClassWrapper(
                session, className, modifiers, classKind,
                primaryConstructor != null,
                classBody.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                toDelegatedSelfType(firClass), delegatedSuperTypeRef ?: defaultDelegatedSuperTypeRef, superTypeCallEntry
            )
            //parse primary constructor
            val firPrimaryConstructor = convertPrimaryConstructor(primaryConstructor, classWrapper)
            firPrimaryConstructor?.let { firClass.declarations += it }

            val properties = mutableListOf<FirProperty>()
            if (primaryConstructor != null && firPrimaryConstructor != null) {
                //parse properties
                properties += primaryConstructor
                    .getChildNodesByType(VALUE_PARAMETER_LIST)
                    .flatMap { convertValueParameters(it) }
                    .filter { it.hasValOrVar() }
                    .map { it.toFirProperty() }
                firClass.addDeclarations(properties)
            }

            //parse declarations
            classBody?.let {
                firClass.addDeclarations(convertClassBody(it, classWrapper))
            }

            //parse data class
            if (modifiers.isDataClass() && firPrimaryConstructor != null) {
                generateComponentFunctions(session, firClass, properties)
                generateCopyFunction(session, firClass, firPrimaryConstructor, properties)
                // TODO: equals, hashCode, toString
            }

            return@withChildClassName firClass
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseObjectLiteral
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitObjectLiteralExpression
     */
    fun convertObjectLiteral(objectLiteral: LighterASTNode): FirElement {
        var modifiers = Modifier(session)
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

        return FirAnonymousObjectImpl(session, null).apply {
            annotations += modifiers.annotations
            this.superTypeRefs += superTypeRefs
            this.typeRef = superTypeRefs.first()

            val classWrapper = ClassWrapper(
                this@DeclarationsConverter.session, SpecialNames.NO_NAME_PROVIDED, modifiers, ClassKind.OBJECT,
                hasPrimaryConstructor = false,
                hasSecondaryConstructor = classBody.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                delegatedSelfTypeRef = delegatedType,
                delegatedSuperTypeRef = delegatedType,
                superTypeCallEntry = superTypeCallEntry
            )
            //parse primary constructor
            val firPrimaryConstructor = convertPrimaryConstructor(primaryConstructor, classWrapper)
            firPrimaryConstructor?.let { this.declarations += it }

            //parse declarations
            classBody?.let {
                this.declarations += convertClassBody(it, classWrapper)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumEntry
     */
    private fun convertEnumEntry(enumEntry: LighterASTNode, classWrapper: ClassWrapper): FirEnumEntryImpl {
        var modifiers = Modifier(session)
        lateinit var identifier: String
        var hasInitializerList = false
        val enumSuperTypeCallEntry = mutableListOf<FirExpression>()
        val firDeclarations = mutableListOf<FirDeclaration>()
        var classBodyNode: LighterASTNode? = null
        enumEntry.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.getAsString()
                INITIALIZER_LIST -> {
                    hasInitializerList = true
                    enumSuperTypeCallEntry += convertInitializerList(it)
                }
                CLASS_BODY -> classBodyNode = it
            }
        }

        val enumEntryName = identifier.nameAsSafeName()
        return ClassNameUtil.withChildClassName(enumEntryName) {
            val firEnumEntry = FirEnumEntryImpl(
                session,
                null,
                FirClassSymbol(ClassNameUtil.currentClassId),
                enumEntryName
            )
            firEnumEntry.annotations += modifiers.annotations

            val defaultDelegatedSuperTypeRef = when {
                modifiers.isEnum() -> implicitEnumType
                modifiers.isAnnotation() -> implicitAnnotationType
                else -> implicitAnyType
            }

            val enumClassWrapper = ClassWrapper(
                session, enumEntryName, modifiers, ClassKind.ENUM_ENTRY,
                hasPrimaryConstructor = true,
                hasSecondaryConstructor = classBodyNode.getChildNodesByType(SECONDARY_CONSTRUCTOR).isNotEmpty(),
                delegatedSelfTypeRef = toDelegatedSelfType(firEnumEntry),
                delegatedSuperTypeRef = if (hasInitializerList) classWrapper.getFirUserTypeFromClassName() else defaultDelegatedSuperTypeRef,
                superTypeCallEntry = enumSuperTypeCallEntry
            )
            firEnumEntry.superTypeRefs += enumClassWrapper.delegatedSuperTypeRef
            convertPrimaryConstructor(null, enumClassWrapper)?.let { firEnumEntry.addDeclaration(it) }
            classBodyNode?.also { firDeclarations += convertClassBody(it, /*classWrapper*/enumClassWrapper) }
            firDeclarations.forEach { firEnumEntry.addDeclaration(it) }

            return@withChildClassName firEnumEntry
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
    private fun convertPrimaryConstructor(primaryConstructor: LighterASTNode?, classWrapper: ClassWrapper): FirConstructorImpl? {
        if (primaryConstructor == null && classWrapper.hasSecondaryConstructor) return null
        if (classWrapper.isInterface()) return null

        var modifiers = Modifier(session)
        val valueParameters = mutableListOf<ValueParameter>()
        primaryConstructor?.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                VALUE_PARAMETER_LIST -> valueParameters += convertValueParameters(it)
            }
        }

        val defaultVisibility = classWrapper.defaultConstructorVisibility()
        val firDelegatedCall = FirDelegatedConstructorCallImpl(
            session,
            null,
            classWrapper.delegatedSuperTypeRef,
            isThis = false
        ).extractArgumentsFrom(classWrapper.superTypeCallEntry, stubMode)

        return FirPrimaryConstructorImpl(
            session,
            null,
            FirConstructorSymbol(ClassNameUtil.callableIdForClassConstructor()),
            if (primaryConstructor != null) modifiers.getVisibility() else defaultVisibility,
            modifiers.hasExpect(),
            modifiers.hasActual(),
            classWrapper.delegatedSelfTypeRef,
            firDelegatedCall
        ).apply {
            annotations += modifiers.annotations
            this.typeParameters += ConverterUtil.typeParametersFromSelfType(classWrapper.delegatedSelfTypeRef)
            this.valueParameters += valueParameters.map { it.firValueParameter }
        }
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
            session,
            null,
            if (stubMode) FirEmptyExpressionBlock(session) else firBlock
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseSecondaryConstructor
     */
    private fun convertSecondaryConstructor(secondaryConstructor: LighterASTNode, classWrapper: ClassWrapper): FirConstructor {
        var modifiers = Modifier(session)
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
            if (classWrapper.isObjectLiteral()) FirErrorTypeRefImpl(session, null, "Constructor in object")
            else classWrapper.delegatedSelfTypeRef

        val firConstructor = FirConstructorImpl(
            session,
            null,
            FirConstructorSymbol(ClassNameUtil.callableIdForClassConstructor()),
            modifiers.getVisibility(),
            modifiers.hasExpect(),
            modifiers.hasActual(),
            delegatedSelfTypeRef,
            constructorDelegationCall
        )

        FunctionUtil.firFunctions += firConstructor
        firConstructor.annotations += modifiers.annotations
        firConstructor.typeParameters += ConverterUtil.typeParametersFromSelfType(delegatedSelfTypeRef)
        firConstructor.valueParameters += firValueParameters.map { it.firValueParameter }
        firConstructor.body = convertFunctionBody(block, null)
        FunctionUtil.firFunctions.removeLast()
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
                CONSTRUCTOR_DELEGATION_REFERENCE -> if (it.getAsString() == "this") thisKeywordPresent = true
                VALUE_ARGUMENT_LIST -> firValueArguments += expressionConverter.convertValueArguments(it)
            }
        }

        val isImplicit = constructorDelegationCall.getAsString().isEmpty()
        val isThis = (isImplicit && classWrapper.hasPrimaryConstructor) || thisKeywordPresent
        val delegatedType =
            if (classWrapper.isObjectLiteral() || classWrapper.isInterface()) when {
                isThis -> FirErrorTypeRefImpl(session, null, "Constructor in object")
                else -> FirErrorTypeRefImpl(session, null, "No super type")
            }
            else when {
                isThis -> classWrapper.delegatedSelfTypeRef
                else -> classWrapper.delegatedSuperTypeRef
            }

        return FirDelegatedConstructorCallImpl(
            session,
            null,
            delegatedType,
            isThis
        ).extractArgumentsFrom(firValueArguments, stubMode)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeAlias
     */
    private fun convertTypeAlias(typeAlias: LighterASTNode): FirDeclaration {
        var modifiers = Modifier(session)
        var identifier: String? = null
        lateinit var firType: FirTypeRef
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        typeAlias.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.getAsString()
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val typeAliasName = identifier.nameAsSafeName()
        return ClassNameUtil.withChildClassName(typeAliasName) {
            return@withChildClassName FirTypeAliasImpl(
                session,
                null,
                FirTypeAliasSymbol(ClassNameUtil.currentClassId),
                typeAliasName,
                modifiers.getVisibility(),
                modifiers.hasExpect(),
                modifiers.hasActual(),
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
        var modifiers = Modifier(session)
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
                IDENTIFIER -> identifier = it.getAsString()
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
            FirVariableImpl(
                session,
                null,
                propertyName,
                returnType,
                isVar,
                firExpression,
                delegate = delegateExpression?.let { expressionConverter.getAsFirExpression(it, "Incorrect delegate expression") }
            ).apply {
                annotations += modifiers.annotations
            }
        } else {
            FirMemberPropertyImpl(
                session,
                null,
                FirPropertySymbol(ClassNameUtil.callableIdForName(propertyName)),
                propertyName,
                modifiers.getVisibility(),
                modifiers.getModality(),
                modifiers.hasExpect(),
                modifiers.hasActual(),
                modifiers.hasOverride(),
                modifiers.isConst(),
                modifiers.hasLateinit(),
                receiverType,
                returnType,
                isVar,
                firExpression,
                getter ?: FirDefaultPropertyGetter(session, null, returnType, modifiers.getVisibility()),
                if (isVar) setter ?: FirDefaultPropertySetter(session, null, returnType, modifiers.getVisibility()) else null,
                delegateExpression?.let { expressionConverter.getAsFirExpression(it, "Should have delegate") }
            ).apply {
                this.typeParameters += firTypeParameters
                this.joinTypeParameters(typeConstraints)
                annotations += modifiers.annotations
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitDestructuringDeclaration
     */
    private fun convertDestructingDeclaration(destructingDeclaration: LighterASTNode): DestructuringDeclaration {
        var isVar = false
        val entries = mutableListOf<FirVariable<*>>()
        var firExpression: FirExpression = FirErrorExpressionImpl(session, null, "Destructuring declaration without initializer")
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
        var modifiers = Modifier(session)
        var identifier: String? = null
        var firType: FirTypeRef? = null
        entry.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                IDENTIFIER -> identifier = it.getAsString()
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        return FirVariableImpl(
            session, null, identifier.nameAsSafeName(), firType ?: implicitType, false, null
        ).apply {
            annotations += modifiers.annotations
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyGetterOrSetter
     */
    private fun convertGetterOrSetter(getterOrSetter: LighterASTNode, propertyTypeRef: FirTypeRef): FirPropertyAccessor {
        var modifiers = Modifier(session)
        var isGetter = true
        var returnType: FirTypeRef? = null
        var firValueParameters: FirValueParameter = FirDefaultSetterValueParameter(session, null, propertyTypeRef)
        var block: LighterASTNode? = null
        var expression: LighterASTNode? = null
        getterOrSetter.forEachChildren {
            if (it.getAsString() == "set") isGetter = false
            when (it.tokenType) {
                SET_KEYWORD -> isGetter = false
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                TYPE_REFERENCE -> returnType = convertType(it)
                VALUE_PARAMETER_LIST -> firValueParameters = convertSetterParameter(it, propertyTypeRef)
                BLOCK -> block = it
                else -> if (it.isExpression()) expression = it
            }
        }

        val firAccessor = FirPropertyAccessorImpl(
            session,
            null,
            isGetter,
            modifiers.getVisibility(),
            returnType ?: if (isGetter) propertyTypeRef else implicitUnitType
        )
        FunctionUtil.firFunctions += firAccessor
        firAccessor.annotations += modifiers.annotations

        if (!isGetter) {
            firAccessor.valueParameters += firValueParameters
        }

        firAccessor.body = convertFunctionBody(block, expression)
        FunctionUtil.firFunctions.removeLast()
        return firAccessor
    }

    /**
     * this is just a VALUE_PARAMETER_LIST
     *
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyGetterOrSetter
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.toFirValueParameter
     */
    private fun convertSetterParameter(setterParameter: LighterASTNode, propertyTypeRef: FirTypeRef): FirValueParameter {
        var modifiers = Modifier(session)
        lateinit var firValueParameter: FirValueParameter
        setterParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifierList(it)
                VALUE_PARAMETER -> firValueParameter = convertValueParameter(it).firValueParameter
            }
        }

        return FirValueParameterImpl(
            session,
            null,
            firValueParameter.name,
            if (firValueParameter.returnTypeRef == implicitType) propertyTypeRef else firValueParameter.returnTypeRef,
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
        var modifiers = Modifier(session)
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
                IDENTIFIER -> identifier = it.getAsString()
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
            FirAnonymousFunctionImpl(session, null, returnType!!, receiverType)
        } else {
            val functionName = identifier.nameAsSafeName()
            FirMemberFunctionImpl(
                session,
                null,
                FirNamedFunctionSymbol(ClassNameUtil.callableIdForName(functionName, isLocal)),
                functionName,
                if (isLocal) Visibilities.LOCAL else modifiers.getVisibility(),
                modifiers.getModality(),
                modifiers.hasExpect(),
                modifiers.hasActual(),
                modifiers.hasOverride(),
                modifiers.hasOperator(),
                modifiers.hasInfix(),
                modifiers.hasInline(),
                modifiers.hasTailrec(),
                modifiers.hasExternal(),
                modifiers.hasSuspend(),
                receiverType,
                returnType!!
            )
        }

        FunctionUtil.firFunctions += firFunction
        firFunction.annotations += modifiers.annotations

        if (firFunction is FirMemberFunctionImpl) {
            firFunction.typeParameters += firTypeParameters
            firFunction.joinTypeParameters(typeConstraints)
        }

        valueParametersList?.let { firFunction.valueParameters += convertValueParameters(it).map { it.firValueParameter } }
        firFunction.body = convertFunctionBody(block, expression)
        FunctionUtil.firFunctions.removeLast()
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
                session,
                expressionConverter.getAsFirExpression<FirExpression>(expression, "Function has no body (but should)").toReturn()
            )
            else -> null
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseBlock
     */
    fun convertBlock(block: LighterASTNode?): FirBlock {
        if (block == null) return FirEmptyExpressionBlock(session)
        if (block.tokenType != BLOCK) {
            return FirSingleExpressionBlock(
                session,
                expressionConverter.getAsFirExpression(block)
            )
        }
        return if (!stubMode) {
            val blockTree = LightTree2Fir.buildLightTreeBlockExpression(block.getAsString())
            return DeclarationsConverter(session, stubMode, blockTree).convertBlockExpression(blockTree.root)
        } else {
            FirSingleExpressionBlock(
                session,
                FirExpressionStub(session, null).toReturn()
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
                CONSTRUCTOR_CALLEE -> if (it.getAsString().isNotEmpty()) firTypeRef = convertType(it)   //is empty in enum entry constructor
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
        var firExpression: FirExpression? = FirErrorExpressionImpl(session, null, "Should have delegate")
        explicitDelegation.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firTypeRef = convertType(it)
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have delegate")
            }
        }

        return FirDelegatedTypeRefImpl(
            session,
            firTypeRef,
            firExpression
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
                REFERENCE_EXPRESSION -> identifier = it.getAsString()
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        return TypeConstraint(annotations, identifier, firType)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeParameter
     */
    private fun convertTypeParameter(typeParameter: LighterASTNode): FirTypeParameter {
        var typeParameterModifiers = TypeParameterModifier(session)
        var identifier: String? = null
        var firType: FirTypeRef? = null
        typeParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> typeParameterModifiers = convertTypeParameterModifiers(it)
                IDENTIFIER -> identifier = it.getAsString()
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val firTypeParameter = FirTypeParameterImpl(
            session,
            null,
            FirTypeParameterSymbol(),
            identifier.nameAsSafeName(),
            typeParameterModifiers.getVariance(),
            typeParameterModifiers.hasReified()
        )
        firTypeParameter.annotations += typeParameterModifiers.annotations
        firType?.let { firTypeParameter.bounds += it }

        return firTypeParameter
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeRef
     */
    fun convertType(type: LighterASTNode): FirTypeRef {
        if (type.getAsString().isEmpty()) {
            return FirErrorTypeRefImpl(session, null, "Unwrapped type is null")
        }
        var typeModifiers = TypeModifier(session) //TODO what with suspend?
        var firType: FirTypeRef = FirErrorTypeRefImpl(session, null, "Incomplete code")
        var afterLPar = false
        type.forEachChildren {
            when (it.tokenType) {
                LPAR -> afterLPar = true
                TYPE_REFERENCE -> firType = convertType(it)
                MODIFIER_LIST -> if (!afterLPar || typeModifiers.hasNoAnnotations()) typeModifiers = convertTypeModifierList(it)
                USER_TYPE -> firType = convertUserType(it)
                NULLABLE_TYPE -> firType = convertNullableType(it)
                FUNCTION_TYPE -> firType = convertFunctionType(it)
                DYNAMIC_TYPE -> firType = FirDynamicTypeRefImpl(session, null, false)
                TokenType.ERROR_ELEMENT -> firType = FirErrorTypeRefImpl(session, null, "Unwrapped type is null")
            }
        }

        return firType.also { (it as FirAbstractAnnotatedTypeRef).annotations += typeModifiers.annotations }
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
                DYNAMIC_TYPE -> firType = FirDynamicTypeRefImpl(session, null, true)
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
                REFERENCE_EXPRESSION -> identifier = it.getAsString()
                TYPE_ARGUMENT_LIST -> firTypeArguments += convertTypeArguments(it)
            }
        }

        if (identifier == null)
            return FirErrorTypeRefImpl(session, null, "Incomplete user type")

        val qualifier = FirQualifierPartImpl(
            identifier.nameAsSafeName()
        ).apply { typeArguments += firTypeArguments }

        return FirUserTypeRefImpl(
            session,
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
        var modifiers = TypeProjectionModifier(session)
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
        return if (isStarProjection) FirStarProjectionImpl(session, null)
        else FirTypeProjectionWithVarianceImpl(
            session,
            null,
            modifiers.getVariance(),
            firType
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
            session,
            null,
            isNullable,
            receiverTypeReference,
            returnTypeReference
        ).apply { valueParameters += valueParametersList.map { it.firValueParameter } }
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
        var modifiers = Modifier(session)
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
                IDENTIFIER -> identifier = it.getAsString()
                TYPE_REFERENCE -> firType = convertType(it)
                DESTRUCTURING_DECLARATION -> destructuringDeclaration = convertDestructingDeclaration(it)
                else -> if (it.isExpression()) firExpression = expressionConverter.getAsFirExpression(it, "Should have default value")
            }
        }

        val firValueParameter = FirValueParameterImpl(
            session,
            null,
            identifier.nameAsSafeName(),
            firType ?: implicitType,
            firExpression,
            isCrossinline = modifiers.hasCrossinline(),
            isNoinline = modifiers.hasNoinline(),
            isVararg = modifiers.hasVararg()
        ).apply { annotations += modifiers.annotations }
        return ValueParameter(isVal, isVar, modifiers, firValueParameter, destructuringDeclaration)
    }
}