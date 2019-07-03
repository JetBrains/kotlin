/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.*
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.nameAsSafeName
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.toDelegatedSelfType
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.hasSecondaryConstructor
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.toReturn
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.joinTypeParameters
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.extractArgumentsFrom
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.fillEnumEntryConstructor
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.fillConstructors
import org.jetbrains.kotlin.fir.lightTree.FunctionUtil.removeLast
import org.jetbrains.kotlin.fir.lightTree.fir.TypeConstraint
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtDotQualifiedExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStringTemplateExpressionElementType
import java.util.stream.Collectors

class Converter(
    val session: FirSession,
    val stubMode: Boolean,
    private val tree: FlyweightCapableTreeStructure<LighterASTNode>
) {

    private val CLASS_MODIFIER = TokenSet.create(ENUM_KEYWORD, ANNOTATION_KEYWORD, DATA_KEYWORD, INNER_KEYWORD, COMPANION_KEYWORD)
    private val MEMBER_MODIFIER = TokenSet.create(OVERRIDE_KEYWORD, LATEINIT_KEYWORD)
    private val VISIBILITY_MODIFIER = TokenSet.create(PUBLIC_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD)
    private val VARIANCE_MODIFIER = TokenSet.create(IN_KEYWORD, OUT_KEYWORD)
    private val FUNCTION_MODIFIER =
        TokenSet.create(TAILREC_KEYWORD, OPERATOR_KEYWORD, INFIX_KEYWORD, INLINE_KEYWORD, EXTERNAL_KEYWORD, SUSPEND_KEYWORD)
    private val PROPERTY_MODIFIER = TokenSet.create(CONST_KEYWORD)
    private val INHERITANCE_MODIFIER = TokenSet.create(ABSTRACT_KEYWORD, FINAL_KEYWORD, OPEN_KEYWORD, SEALED_KEYWORD)
    private val PARAMETER_MODIFIER = TokenSet.create(VARARG_KEYWORD, NOINLINE_KEYWORD, CROSSINLINE_KEYWORD)
    private val REIFICATION_MODIFIER = TokenSet.create(REIFIED_KEYWORD)
    private val PLATFORM_MODIFIER = TokenSet.create(EXPECT_KEYWORD, ACTUAL_KEYWORD)

    private val implicitUnitType = FirImplicitUnitTypeRef(session, null)
    private val implicitAnyType = FirImplicitAnyTypeRef(session, null)
    private val implicitEnumType = FirImplicitEnumTypeRef(session, null)
    private val implicitAnnotationType = FirImplicitAnnotationTypeRef(session, null)
    private val implicitType = FirImplicitTypeRefImpl(session, null)

    private fun LighterASTNode?.getChildrenAsArray(): Array<LighterASTNode?> {
        if (this == null) return arrayOf()

        val kidsRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, kidsRef)
        return kidsRef.get()
    }

    private inline fun LighterASTNode.forEachChildren(f: (LighterASTNode) -> Unit) {
        val kidsArray = this.getChildrenAsArray()
        for (kid in kidsArray) {
            if (kid == null) continue
            val tokenType = kid.tokenType
            if (COMMENTS.contains(tokenType) || tokenType == WHITE_SPACE || tokenType == SEMICOLON) continue
            f(kid)
        }
    }

    private inline fun <T> LighterASTNode.forEachChildrenReturnList(f: (LighterASTNode, MutableList<T>) -> Unit): List<T> {
        val kidsArray = this.getChildrenAsArray()

        val container = mutableListOf<T>()
        for (kid in kidsArray) {
            if (kid == null) continue
            val tokenType = kid.tokenType
            if (COMMENTS.contains(tokenType) || tokenType == WHITE_SPACE || tokenType == SEMICOLON) continue
            f(kid, container)
        }

        return container
    }

    fun convertFile(file: LighterASTNode, fileName: String = ""): FirFile {
        val tokenType = file.tokenType
        if (tokenType !is IFileElementType) {
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
     * [org.jetbrains.kotlin.parsing.KotlinParsing.parseFileAnnotationList]
     */
    private fun convertFileAnnotationList(fileAnnotationList: LighterASTNode): List<FirAnnotationCall> {
        return fileAnnotationList.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                ANNOTATION -> container += convertAnnotation(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePackageName
     */
    private fun convertPackageName(packageNode: LighterASTNode): FqName {
        var packageName: FqName = FqName.ROOT
        packageNode.forEachChildren {
            when (it.tokenType) {
                DOT_QUALIFIED_EXPRESSION, REFERENCE_EXPRESSION -> packageName = FqName(it.toString())
            }
        }
        return packageName
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

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseImportDirective
     */
    private fun convertImportDirective(importDirective: LighterASTNode): FirImport {
        var importedFqName: FqName? = null
        var isAllUnder: Boolean = false
        var aliasName: String? = null
        importDirective.forEachChildren {
            when (it.tokenType) {
                DOT_QUALIFIED_EXPRESSION, REFERENCE_EXPRESSION -> {
                    val importName = it.toString()
                    if (importName.endsWith(".*")) {
                        isAllUnder = true
                        importName.replace(".*", "")
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
            aliasName.nameAsSafeName()
        )
    }

    private fun convertImportAlias(importAlias: LighterASTNode): String {
        importAlias.forEachChildren {
            when (it.tokenType) {
                IDENTIFIER -> return it.toString()
            }
        }

        //TODO specify error
        throw Exception()
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassOrObject
     */
    private fun convertClass(classNode: LighterASTNode): FirDeclaration {
        var modifiers = Modifier(session)
        lateinit var classKind: ClassKind
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
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                CLASS_KEYWORD -> classKind = ClassKind.CLASS
                INTERFACE_KEYWORD -> classKind = ClassKind.INTERFACE
                OBJECT_KEYWORD -> classKind = ClassKind.OBJECT
                IDENTIFIER -> identifier = it.toString()
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

        classKind = when (modifiers.classModifier) {
            ClassModifier.ENUM -> ClassKind.ENUM_CLASS
            ClassModifier.ANNOTATION -> ClassKind.ANNOTATION_CLASS
            else -> classKind
        }
        val hasSecondaryConstructor = classBody.hasSecondaryConstructor(classBody.getChildrenAsArray())
        val className = identifier.nameAsSafeName(if (modifiers.classModifier == ClassModifier.COMPANION) "Companion" else "")

        val defaultDelegatedSuperTypeRef = when (classKind) {
            ClassKind.ENUM_CLASS -> implicitEnumType
            ClassKind.ANNOTATION_CLASS -> implicitAnnotationType
            else -> implicitAnyType
        }
        val delegatedSelfTypeRef = className.toDelegatedSelfType(session)
        delegatedSuperTypeRef = delegatedSuperTypeRef ?: defaultDelegatedSuperTypeRef

        return ClassNameUtil.withChildClassName(className) {
            val firClass = FirClassImpl(
                session,
                null,
                FirClassSymbol(ClassNameUtil.currentClassId),
                className,
                modifiers.visibilityModifier.toVisibility(),
                modifiers.inheritanceModifier?.toModality(),
                modifiers.platformModifier == PlatformModifier.EXPECT,
                modifiers.platformModifier == PlatformModifier.ACTUAL,
                classKind,
                isInner = modifiers.classModifier == ClassModifier.INNER,
                isCompanion = modifiers.classModifier == ClassModifier.COMPANION,
                isData = modifiers.classModifier == ClassModifier.DATA,
                isInline = modifiers.functionModifier == FunctionModifier.INLINE
            )
            firClass.annotations += modifiers.annotations
            firClass.typeParameters += firTypeParameters
            firClass.joinTypeParameters(typeConstraints)
            firClass.superTypeRefs += superTypeRefs

            var firPrimaryConstructor: FirConstructor? = null
            if (classKind != ClassKind.INTERFACE && !(primaryConstructor == null && hasSecondaryConstructor)) {
                val firDelegatedConstructorCall = convertConstructorDelegationCall(null)
                    .extractArgumentsFrom(superTypeCallEntry, stubMode)
                firPrimaryConstructor = convertPrimaryConstructor(primaryConstructor)
                    .apply {
                        this.returnTypeRef = delegatedSelfTypeRef
                        this.delegatedConstructor = firDelegatedConstructorCall
                        firClass.declarations += this
                    }
            }
            getValueParameters(primaryConstructor).forEach {
                if (it.hasValOrVar()) {
                    firClass.declarations += it.toFirProperty()
                }
            }

            classBody?.let {
                if (classKind == ClassKind.ENUM_CLASS) {
                    // separate converter only for enum entries
                    val firEnumEntries = convertEnumClassBody(it) //return enum entries list
                    firEnumEntries.fillEnumEntryConstructor(firPrimaryConstructor!!, stubMode)
                    firClass.declarations += firEnumEntries
                }
                firClass.declarations += convertClassBody(it)
            }

            firClass.declarations.stream()
                .filter { it is FirConstructor }
                .collect(Collectors.toList())
                .fillConstructors(primaryConstructor != null, delegatedSelfTypeRef, delegatedSuperTypeRef!!)

            return@withChildClassName firClass
        }
    }

    //TODO try to get rid of this method
    private fun getValueParameters(primaryConstructor: LighterASTNode?): List<ValueParameter> {
        if (primaryConstructor == null) return listOf()

        return primaryConstructor.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_PARAMETER_LIST -> container += convertClassParameters(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassOrObject
     * primaryConstructor branch
     */
    private fun convertPrimaryConstructor(primaryConstructor: LighterASTNode?): FirConstructorImpl {
        //if (primaryConstructor == null && hasSecondaryConstructor) return null

        var modifiers = Modifier(session)
        val valueParameters = mutableListOf<ValueParameter>()
        primaryConstructor?.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                VALUE_PARAMETER_LIST -> valueParameters += convertClassParameters(it)
            }
        }

        return FirPrimaryConstructorImpl(
            session,
            null,
            FirFunctionSymbol(ClassNameUtil.callableIdForClassConstructor()),
            modifiers.visibilityModifier.toVisibility(),
            modifiers.platformModifier == PlatformModifier.EXPECT,
            modifiers.platformModifier == PlatformModifier.ACTUAL,
            implicitType,            //must be initialized with "delegatedSelfTypeRef" outside of this method
            null    //must be initialized outside of this method
        ).apply {
            annotations += modifiers.annotations
            this.valueParameters += valueParameters.map { it.firValueParameter }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameterList
     */
    private fun convertClassParameters(classParameters: LighterASTNode): List<ValueParameter> {
        return classParameters.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_PARAMETER -> container += convertClassParameter(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameter
     */
    private fun convertClassParameter(classParameter: LighterASTNode): ValueParameter {
        var modifiers = Modifier(session)
        var isVal = false
        var isVar = false
        lateinit var identifier: String
        lateinit var firType: FirTypeRef
        var firExpression: FirExpression? = null
        classParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                VAL_KEYWORD -> isVal = true
                VAR_KEYWORD -> isVar = true
                IDENTIFIER -> identifier = it.toString()
                TYPE_REFERENCE -> firType = convertType(it)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType -> firExpression = visitExpression(it)
            }
        }

        val firValueParameter = FirValueParameterImpl(
            session,
            null,
            Name.identifier(identifier),
            firType,
            firExpression,
            isCrossinline = modifiers.parameterModifier == ParameterModifier.CROSSINLINE,
            isNoinline = modifiers.parameterModifier == ParameterModifier.NOINLINE,
            isVararg = modifiers.parameterModifier == ParameterModifier.VARARG
        ).apply { annotations += modifiers.annotations }
        return ValueParameter(isVal, isVar, modifiers, firValueParameter)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseDelegationSpecifierList
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.extractSuperTypeListEntriesTo
     *
     * SUPER_TYPE_ENTRY             - userType
     * SUPER_TYPE_CALL_ENTRY        - constructorInvocation
     * DELEGATED_SUPER_TYPE_ENTRY   - explicitDelegation
     */
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
        lateinit var firTypeRef: FirTypeRef
        val firValueArguments = mutableListOf<FirExpression>()
        constructorInvocation.forEachChildren {
            when (it.tokenType) {
                CONSTRUCTOR_CALLEE -> firTypeRef = convertType(it)
                VALUE_ARGUMENT_LIST -> firValueArguments += convertValueArguments(it)
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
        var expression: FirExpression? = null
        explicitDelegation.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firTypeRef = convertType(it)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType -> expression = visitExpression(it) // TODO implement
            }
        }

        return FirDelegatedTypeRefImpl(
            session,
            firTypeRef,
            expression
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseClassBody
     */
    private fun convertClassBody(classBody: LighterASTNode): List<FirDeclaration> {
        return classBody.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                CLASS -> container += convertClass(node)
                FUN -> container += convertFunctionDeclaration(node)
                PROPERTY -> container += convertPropertyDeclaration(node)
                TYPEALIAS -> container += convertTypeAlias(node)
                OBJECT_DECLARATION -> container += convertClass(node)
                CLASS_INITIALIZER -> container += convertAnonymousInitializer(node) //anonymousInitializer
                SECONDARY_CONSTRUCTOR -> container += convertSecondaryConstructor(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.convert(
     * KtConstructorDelegationCall, FirTypeRef, Boolean)
     */
    private fun convertConstructorDelegationCall(constructorDelegationCall: LighterASTNode?): FirDelegatedConstructorCallImpl {
        var isThis: Boolean = false
        var isSuper: Boolean = false
        constructorDelegationCall?.forEachChildren {
            when (it.tokenType) {
                CONSTRUCTOR_DELEGATION_REFERENCE -> {
                    if (it.toString() == "this") isThis = true
                    if (it.toString() == "super") isSuper = true
                }
                VALUE_ARGUMENT_LIST -> "" //TODO implement
            }
        }

        return FirDelegatedConstructorCallImpl(
            session,
            null,
            implicitType,
            isThis
        ).extractArgumentsFrom(listOf(), stubMode) //TODO implement
    }

    /**
     * Parse only enum entries, without members
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumClassBody
     */
    private fun convertEnumClassBody(classBody: LighterASTNode): List<FirEnumEntryImpl> {
        return classBody.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                ENUM_ENTRY -> container += convertEnumEntry(node)
            }
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
                BLOCK -> firBlock = visitBlock(it)
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
    private fun convertSecondaryConstructor(secondaryConstructor: LighterASTNode): FirConstructor {
        var modifiers = Modifier(session)
        val firValueParameters = mutableListOf<FirValueParameter>()
        var constructorDelegationCall: FirDelegatedConstructorCall? = null
        var firBlock: FirBlock? = null

        secondaryConstructor.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                VALUE_PARAMETER_LIST -> firValueParameters += convertFunctionValueParameters(it)
                CONSTRUCTOR_DELEGATION_CALL -> constructorDelegationCall = convertConstructorDelegationCall(it)
                BLOCK -> firBlock = visitBlock(it)
            }
        }

        val firConstructor = FirConstructorImpl(
            session,
            null,
            FirFunctionSymbol(ClassNameUtil.callableIdForClassConstructor()),
            modifiers.visibilityModifier.toVisibility(),
            modifiers.platformModifier == PlatformModifier.EXPECT,
            modifiers.platformModifier == PlatformModifier.ACTUAL,
            implicitType,               //must be initialized with "delegatedSelfTypeRef" outside of this method
            constructorDelegationCall
        )
        FunctionUtil.firFunctions += firConstructor
        firConstructor.annotations += modifiers.annotations
        firConstructor.valueParameters += firValueParameters
        firConstructor.body = visitFunctionBody(firBlock, null)
        FunctionUtil.firFunctions.removeLast()
        return firConstructor
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseEnumEntry
     */
    private fun convertEnumEntry(enumEntry: LighterASTNode): FirEnumEntryImpl {
        var modifiers = Modifier(session)
        lateinit var identifier: String
        var initializerList = null
        val firDeclarations = mutableListOf<FirDeclaration>()
        enumEntry.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                INITIALIZER_LIST -> "" //TODO implement
                CLASS_BODY -> firDeclarations += convertClassBody(it)
            }
        }

        val enumEntryName = Name.identifier(identifier)
        return ClassNameUtil.withChildClassName(enumEntryName) {
            return@withChildClassName FirEnumEntryImpl(
                session,
                null,
                FirClassSymbol(ClassNameUtil.currentClassId),
                enumEntryName
            ).apply {
                annotations += modifiers.annotations
                declarations += firDeclarations
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunction
     */
    private fun convertFunctionDeclaration(functionDeclaration: LighterASTNode): FirDeclaration {
        var modifiers = Modifier(session)
        lateinit var identifier: String
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        val valueParametersList = mutableListOf<FirValueParameter>()
        var isReturnType = false
        var receiverType: FirTypeRef? = null
        var returnType: FirTypeRef? = null
        val typeConstraints = mutableListOf<TypeConstraint>()
        var firBlock: FirBlock? = null
        var firExpression: FirExpression? = null
        functionDeclaration.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                VALUE_PARAMETER_LIST -> valueParametersList += convertFunctionValueParameters(it)
                COLON -> isReturnType = true
                TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverType = convertType(it)
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                BLOCK -> firBlock = visitBlock(it)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType,
                REFERENCE_EXPRESSION -> firExpression = visitExpression(it) //TODO implement
            }
        }

        if (returnType == null) {
            returnType =
                if (firBlock != null || (firBlock == null && firExpression == null)) implicitUnitType
                else implicitType
        }

        val functionName = Name.identifier(identifier)
        val firFunction = FirMemberFunctionImpl(
            session,
            null,
            FirFunctionSymbol(ClassNameUtil.callableIdForName(functionName)),
            functionName,
            modifiers.visibilityModifier.toVisibility(),
            modifiers.inheritanceModifier?.toModality(),
            modifiers.platformModifier == PlatformModifier.EXPECT,
            modifiers.platformModifier == PlatformModifier.ACTUAL,
            modifiers.memberModifier == MemberModifier.OVERRIDE,
            modifiers.functionModifier == FunctionModifier.OPERATOR,
            modifiers.functionModifier == FunctionModifier.INFIX,
            modifiers.functionModifier == FunctionModifier.INLINE,
            modifiers.functionModifier == FunctionModifier.TAILREC,
            modifiers.functionModifier == FunctionModifier.EXTERNAL,
            modifiers.functionModifier == FunctionModifier.SUSPEND,
            receiverType,
            returnType!!
        )

        FunctionUtil.firFunctions += firFunction
        firFunction.annotations += modifiers.annotations

        firFunction.typeParameters += firTypeParameters
        firFunction.joinTypeParameters(typeConstraints)

        firFunction.valueParameters += valueParametersList
        firFunction.body = visitFunctionBody(firBlock, firExpression)
        FunctionUtil.firFunctions.removeLast()
        return firFunction
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameterList
     */
    private fun convertFunctionValueParameters(functionValueParameters: LighterASTNode): List<FirValueParameter> {
        return functionValueParameters.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_PARAMETER -> container += convertFunctionValueParameter(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameter
     */
    private fun convertFunctionValueParameter(functionValueParameter: LighterASTNode): FirValueParameter {
        var modifiers = Modifier(session)
        lateinit var identifier: String
        lateinit var firType: FirTypeRef
        var firExpression: FirExpression? = null
        functionValueParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_REFERENCE -> firType = convertType(it)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType,
                REFERENCE_EXPRESSION -> firExpression = visitExpression(it) //TODO implement
            }
        }

        return FirValueParameterImpl(
            session,
            null,
            Name.identifier(identifier),
            firType,
            firExpression,
            isCrossinline = modifiers.parameterModifier == ParameterModifier.CROSSINLINE,
            isNoinline = modifiers.parameterModifier == ParameterModifier.NOINLINE,
            isVararg = modifiers.parameterModifier == ParameterModifier.VARARG
        ).apply { annotations += modifiers.annotations }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionParameterRest
     */
    private fun convertParameter(parameter: LighterASTNode): FirValueParameter {
        var identifier: String? = null
        var firType: FirTypeRef? = null
        parameter.forEachChildren {
            when (it.tokenType) {
                IDENTIFIER -> identifier = it.toString()
                TYPE_REFERENCE -> firType = convertType(it)
                //TODO expression?
            }
        }

        return FirValueParameterImpl(
            session,
            null,
            identifier?.let { Name.identifier(it) } ?: SpecialNames.NO_NAME_PROVIDED,
            firType ?: implicitType,
            null,
            isCrossinline = false,
            isNoinline = false,
            isVararg = false
        )
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
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                VALUE_PARAMETER -> firValueParameter = convertParameter(it)
            }
        }

        return FirValueParameterImpl(
            session,
            null,
            firValueParameter.name,
            if (firValueParameter.returnTypeRef == implicitType) propertyTypeRef else firValueParameter.returnTypeRef,
            null,//TODO implement
            isCrossinline = modifiers.parameterModifier == ParameterModifier.CROSSINLINE,
            isNoinline = modifiers.parameterModifier == ParameterModifier.NOINLINE,
            isVararg = modifiers.parameterModifier == ParameterModifier.VARARG
        ).apply {
            annotations += modifiers.annotations
        }
    }

    private fun visitFunctionBody(firBlock: FirBlock?, firExpression: FirExpression?): FirBlock? {
        return when {
            firBlock != null -> if (!stubMode) {
                return firBlock
            } else {
                FirSingleExpressionBlock(
                    session,
                    FirExpressionStub(session, null).toReturn()
                )
            }
            firExpression != null -> {
                FirSingleExpressionBlock(
                    session,
                    firExpression.toReturn()
                )
            }
            else -> null
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseProperty
     */
    private fun convertPropertyDeclaration(property: LighterASTNode): FirDeclaration {
        //TODO DESTRUCTURING_DECLARATION
        var modifiers = Modifier(session)
        lateinit var identifier: String
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        var isReturnType = false
        var isDelegate = false
        var isVar = false
        var receiverType: FirTypeRef? = null
        var returnType: FirTypeRef = implicitType
        val typeConstraints = mutableListOf<TypeConstraint>()
        var getter: FirPropertyAccessor? = null
        var setter: FirPropertyAccessor? = null
        var firExpression: FirExpression? = null
        property.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                COLON -> isReturnType = true
                TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverType = convertType(it)
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                PROPERTY_DELEGATE -> isDelegate = true
                VAR_KEYWORD -> isVar = true
                PROPERTY_ACCESSOR ->
                    if (it.toString().contains("get")) //TODO make it better
                        getter = convertGetter(it, returnType)
                    else
                        setter = convertSetter(it, returnType)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType,
                REFERENCE_EXPRESSION -> firExpression = visitExpression(it) //TODO implement
            }
        }

        val propertyName = Name.identifier(identifier)

        return if (FunctionUtil.firFunctions.isNotEmpty()) {
            FirVariableImpl(
                session,
                null,
                propertyName,
                returnType,
                isVar,
                firExpression,
                delegate = if (isDelegate) {
                    FirExpressionStub(session, null)
                    //TODO("not implemented")
                    //{ property.delegate?.expression }.toFirExpression("Should have delegate")
                } else null
            )
        } else {
            FirMemberPropertyImpl(
                session,
                null,
                FirPropertySymbol(ClassNameUtil.callableIdForName(propertyName)),
                propertyName,
                modifiers.visibilityModifier.toVisibility(),
                modifiers.inheritanceModifier?.toModality(),
                modifiers.platformModifier == PlatformModifier.EXPECT,
                modifiers.platformModifier == PlatformModifier.ACTUAL,
                modifiers.memberModifier == MemberModifier.OVERRIDE,
                modifiers.propertyModifier == PropertyModifier.CONST,
                modifiers.memberModifier == MemberModifier.LATEINIT,
                receiverType,
                returnType,
                isVar,
                firExpression,
                getter ?: FirDefaultPropertyGetter(session, null, returnType, modifiers.visibilityModifier.toVisibility()),
                setter ?: FirDefaultPropertySetter(session, null, returnType, modifiers.visibilityModifier.toVisibility()),
                if (isDelegate) {
                    FirExpressionStub(session, null)
                    //TODO("not implemented")
                    //{ property.delegate?.expression }.toFirExpression("Should have delegate")
                } else null
            ).apply {
                this.typeParameters += firTypeParameters
                this.joinTypeParameters(typeConstraints)
                annotations += modifiers.annotations
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyGetterOrSetter
     */
    private fun convertGetter(getter: LighterASTNode, propertyTypeRef: FirTypeRef): FirPropertyAccessor {
        var modifiers = Modifier(session)

        var returnType: FirTypeRef? = null
        var firBlock: FirBlock? = null
        var firExpression: FirExpression? = null
        getter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                TYPE_REFERENCE -> returnType = convertType(it)
                BLOCK -> firBlock = visitBlock(it)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType,
                REFERENCE_EXPRESSION -> firExpression = visitExpression(it) //TODO implement
            }
        }

        val firAccessor = FirPropertyAccessorImpl(
            session,
            null,
            true,
            modifiers.visibilityModifier.toVisibility(),
            returnType ?: propertyTypeRef
        )
        FunctionUtil.firFunctions += firAccessor
        firAccessor.annotations += modifiers.annotations

        firAccessor.body = visitFunctionBody(firBlock, firExpression)
        FunctionUtil.firFunctions.removeLast()
        return firAccessor
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parsePropertyGetterOrSetter
     */
    private fun convertSetter(setter: LighterASTNode, propertyTypeRef: FirTypeRef): FirPropertyAccessor {
        var modifiers = Modifier(session)

        var returnType: FirTypeRef? = null
        val firValueParameters = mutableListOf<FirValueParameter>()
        var firBlock: FirBlock? = null
        var firExpression: FirExpression? = null
        setter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                TYPE_REFERENCE -> returnType = convertType(it)
                VALUE_PARAMETER_LIST -> firValueParameters += convertSetterParameter(it, propertyTypeRef)
                BLOCK -> firBlock = visitBlock(it)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType,
                REFERENCE_EXPRESSION -> firExpression = visitExpression(it) //TODO implement
            }
        }

        val firAccessor = FirPropertyAccessorImpl(
            session,
            null,
            false,
            modifiers.visibilityModifier.toVisibility(),
            returnType ?: implicitUnitType
        )
        FunctionUtil.firFunctions += firAccessor
        firAccessor.annotations += modifiers.annotations

        firValueParameters.ifEmpty {
            firValueParameters +=
                FirDefaultSetterValueParameter(
                    session,
                    null,
                    propertyTypeRef
                )
        }
        firAccessor.valueParameters += firValueParameters

        firAccessor.body = visitFunctionBody(firBlock, firExpression)
        FunctionUtil.firFunctions.removeLast()
        return firAccessor
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeAlias
     */
    private fun convertTypeAlias(typeAlias: LighterASTNode): FirDeclaration {
        var modifiers = Modifier(session)
        lateinit var identifier: String
        lateinit var firType: FirTypeRef
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        typeAlias.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val typeAliasName = Name.identifier(identifier)
        return ClassNameUtil.withChildClassName(typeAliasName) {
            return@withChildClassName FirTypeAliasImpl(
                session,
                null,
                FirTypeAliasSymbol(ClassNameUtil.currentClassId),
                typeAliasName,
                modifiers.visibilityModifier.toVisibility(),
                modifiers.platformModifier == PlatformModifier.EXPECT,
                modifiers.platformModifier == PlatformModifier.ACTUAL,
                firType
            ).apply {
                annotations += modifiers.annotations
                typeParameters += firTypeParameters
            }
        }
    }

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
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeParameter
     */
    private fun convertTypeParameter(typeParameter: LighterASTNode): FirTypeParameter {
        var typeParameterModifiers = TypeParameterModifier(session)
        lateinit var identifier: String
        var firType: FirTypeRef? = null
        typeParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> typeParameterModifiers = convertTypeParameterModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val firTypeParameter = FirTypeParameterImpl(
            session,
            null,
            FirTypeParameterSymbol(),
            identifier.nameAsSafeName(),
            typeParameterModifiers.varianceModifier.toVariance(),
            typeParameterModifiers.reificationModifier != null
        )
        firTypeParameter.annotations += typeParameterModifiers.annotations
        firType?.let { firTypeParameter.bounds += it }

        return firTypeParameter
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertTypeParameterModifiers(typeParameterModifiers: LighterASTNode): TypeParameterModifier {
        val modifier = TypeParameterModifier(session)
        typeParameterModifiers.forEachChildren {
            val tokenType = it.tokenType
            when {
                VARIANCE_MODIFIER.contains(tokenType) -> modifier.varianceModifier = convertVarianceModifier(it)
                REIFICATION_MODIFIER.contains(tokenType) -> modifier.reificationModifier = convertReificationModifier(it)
                tokenType == ANNOTATION -> modifier.annotations += convertAnnotation(it)
                tokenType == ANNOTATION_ENTRY -> modifier.annotations += convertUnescapedAnnotation(it, null)
            }
        }
        return modifier
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeRef
     */
    private fun convertType(type: LighterASTNode): FirTypeRef {
        var typeModifiers = TypeModifier(session) //TODO what with suspend?
        lateinit var firType: FirTypeRef
        type.forEachChildren { it ->
            when (it.tokenType) {
                TYPE_REFERENCE -> firType = convertType(it)
                MODIFIER_LIST -> typeModifiers = convertTypeModifiers(it)
                USER_TYPE -> firType = convertUserType(it).also { firUserType -> (firUserType as FirUserTypeRefImpl).qualifier.reverse() }
                NULLABLE_TYPE -> firType = convertNullableType(it)
                FUNCTION_TYPE -> firType = convertFunctionType(it)
                DYNAMIC_TYPE -> firType = FirDynamicTypeRefImpl(session, null, false)
            }
        }

        return firType.also { (it as FirAbstractAnnotatedTypeRef).annotations += typeModifiers.annotations }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertTypeModifiers(typeModifiers: LighterASTNode): TypeModifier {
        val modifier = TypeModifier(session)
        typeModifiers.forEachChildren {
            val tokenType = it.tokenType
            when {
                it.toString() == SUSPEND_KEYWORD.value -> modifier.suspendModifier = SuspendModifier.SUSPEND
                tokenType == ANNOTATION -> modifier.annotations += convertAnnotation(it)
                tokenType == ANNOTATION_ENTRY -> modifier.annotations += convertUnescapedAnnotation(it, null)
            }
        }
        return modifier
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseNullableTypeSuffix
     */
    private fun convertNullableType(nullableType: LighterASTNode): FirTypeRef {
        lateinit var firType: FirTypeRef
        nullableType.forEachChildren {
            when (it.tokenType) {
                USER_TYPE -> firType = convertUserType(it, true)
                FUNCTION_TYPE -> firType = convertFunctionType(it, true)
                NULLABLE_TYPE -> firType = convertNullableType(it)
                DYNAMIC_TYPE -> firType = FirDynamicTypeRefImpl(session, null, true)
            }
        }

        return firType
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseFunctionType
     */
    private fun convertFunctionType(functionType: LighterASTNode, isNullable: Boolean = false): FirTypeRef {
        var receiverTypeReference: FirTypeRef? = null
        lateinit var returnTypeReference: FirTypeRef
        val valueParametersList = mutableListOf<FirValueParameter>()
        functionType.forEachChildren {
            when (it.tokenType) {
                FUNCTION_TYPE_RECEIVER -> receiverTypeReference = convertReceiverType(it)
                VALUE_PARAMETER_LIST -> valueParametersList += convertFunctionTypeParameters(it)
                TYPE_REFERENCE -> returnTypeReference = convertType(it)
            }
        }

        return FirFunctionTypeRefImpl(
            session,
            null,
            isNullable,
            receiverTypeReference,
            returnTypeReference
        ).apply { valueParameters += valueParametersList }
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
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseUserType
     */
    private fun convertUserType(userType: LighterASTNode, isNullable: Boolean = false): FirUserTypeRef {
        var simpleFirUserType: FirUserTypeRef? = null
        lateinit var identifier: String
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        userType.forEachChildren {
            when (it.tokenType) {
                USER_TYPE -> simpleFirUserType = convertUserType(it) //simple user type
                REFERENCE_EXPRESSION -> identifier = it.toString()
                TYPE_ARGUMENT_LIST -> firTypeArguments += convertTypeArguments(it)
            }
        }

        val qualifier = FirQualifierPartImpl(
            Name.identifier(identifier)
        ).apply { typeArguments += firTypeArguments }

        return FirUserTypeRefImpl(
            session,
            null,
            isNullable
        ).apply {
            this.qualifier.add(qualifier)
            simpleFirUserType?.qualifier?.let { this@apply.qualifier.addAll(it) }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseValueParameter
     */
    private fun convertFunctionTypeParameters(functionTypeParameters: LighterASTNode): List<FirValueParameter> {
        return functionTypeParameters.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_PARAMETER -> container += convertParameter(node)
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
                //TODO check annotations
                ANNOTATION, ANNOTATION_ENTRY -> annotations += convertAnnotation(it)
                REFERENCE_EXPRESSION -> identifier = it.toString()
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        return TypeConstraint(annotations, identifier, firType)
    }

    //TODO move to parsing class?
    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseValueArgumentList
     */
    private fun convertValueArguments(valueArguments: LighterASTNode): List<FirExpression> {
        return valueArguments.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_ARGUMENT -> container += convertValueArgument(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseTypeArgumentList
     */
    private fun convertTypeArguments(typeArguments: LighterASTNode): List<FirTypeProjection> {
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
                MODIFIER_LIST -> modifiers = convertTypeProjectionModifiers(it)
                TYPE_REFERENCE -> firType = convertType(it)
                MUL -> isStarProjection = true
            }
        }

        //TODO what with annotations?
        return if (isStarProjection) FirStarProjectionImpl(session, null)
        else FirTypeProjectionWithVarianceImpl(
            session,
            null,
            modifiers.varianceModifier.toVariance(),
            firType
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertTypeProjectionModifiers(modifiers: LighterASTNode): TypeProjectionModifier {
        val modifier = TypeProjectionModifier(session)
        modifiers.forEachChildren {
            val tokenType = it.tokenType
            when {
                VARIANCE_MODIFIER.contains(tokenType) -> modifier.varianceModifier = convertVarianceModifier(it)
                tokenType == ANNOTATION -> modifier.annotations += convertAnnotation(it)
                tokenType == ANNOTATION_ENTRY -> modifier.annotations += convertUnescapedAnnotation(it, null)
            }
        }
        return modifier
    }

    private fun convertValueArgument(valueArgument: LighterASTNode): FirExpression {
        return FirErrorExpressionImpl(session, null, "Not implemented")
        //TODO implement

        /*
        this ?: return FirErrorExpressionImpl(session, this as? KtElement, "No argument given")
            val expression = this.getArgumentExpression()
            return when (expression) {
                is KtConstantExpression, is KtStringTemplateExpression -> {
                    expression.accept(this@Visitor, Unit) as FirExpression
                }

                else -> {
                    { expression }.toFirExpression("Argument is absent")
                }
            }
         */
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinParsing.parseModifierList
     */
    private fun convertModifiers(modifiers: LighterASTNode): Modifier {
        val modifier = Modifier(session)
        modifiers.forEachChildren {
            val tokenType = it.tokenType
            when {
                CLASS_MODIFIER.contains(tokenType) -> modifier.classModifier = convertClassModifier(it)
                MEMBER_MODIFIER.contains(tokenType) -> modifier.memberModifier = convertMemberModifier(it)
                VISIBILITY_MODIFIER.contains(tokenType) -> modifier.visibilityModifier = convertVisibilityModifier(it)
                FUNCTION_MODIFIER.contains(tokenType) -> modifier.functionModifier = convertFunctionModifier(it)
                PROPERTY_MODIFIER.contains(tokenType) -> modifier.propertyModifier = convertPropertyModifier(it)
                INHERITANCE_MODIFIER.contains(tokenType) -> modifier.inheritanceModifier = convertInheritanceModifier(it)
                PARAMETER_MODIFIER.contains(tokenType) -> modifier.parameterModifier = convertParameterModifier(it)
                PLATFORM_MODIFIER.contains(tokenType) -> modifier.platformModifier = convertPlatformModifier(it)
                tokenType == ANNOTATION -> modifier.annotations += convertAnnotation(it)
                tokenType == ANNOTATION_ENTRY -> modifier.annotations += convertUnescapedAnnotation(it, null)
            }
        }
        return modifier
    }

    private fun convertClassModifier(classModifier: LighterASTNode): ClassModifier {
        return ClassModifier.valueOf(classModifier.toString().toUpperCase())
    }

    private fun convertMemberModifier(memberModifier: LighterASTNode): MemberModifier {
        return MemberModifier.valueOf(memberModifier.toString().toUpperCase())
    }

    private fun convertVisibilityModifier(visibilityModifier: LighterASTNode): VisibilityModifier {
        return VisibilityModifier.valueOf(visibilityModifier.toString().toUpperCase())
    }

    private fun convertVarianceModifier(varianceModifier: LighterASTNode): VarianceModifier {
        return VarianceModifier.valueOf(varianceModifier.toString().toUpperCase())
    }

    private fun convertFunctionModifier(functionModifier: LighterASTNode): FunctionModifier {
        return FunctionModifier.valueOf(functionModifier.toString().toUpperCase())
    }

    private fun convertPropertyModifier(propertyModifier: LighterASTNode): PropertyModifier {
        return PropertyModifier.valueOf(propertyModifier.toString().toUpperCase())
    }

    private fun convertInheritanceModifier(inheritanceModifier: LighterASTNode): InheritanceModifier {
        return InheritanceModifier.valueOf(inheritanceModifier.toString().toUpperCase())
    }

    private fun convertParameterModifier(parameterModifier: LighterASTNode): ParameterModifier {
        return ParameterModifier.valueOf(parameterModifier.toString().toUpperCase())
    }

    private fun convertReificationModifier(reificationModifier: LighterASTNode): ReificationModifier {
        return ReificationModifier.valueOf(reificationModifier.toString().toUpperCase())
    }

    private fun convertPlatformModifier(platformModifier: LighterASTNode): PlatformModifier {
        return PlatformModifier.valueOf(platformModifier.toString().toUpperCase())
    }

    private fun convertAnnotation(annotationNode: LighterASTNode): List<FirAnnotationCall> {
        var annotationTarget: AnnotationUseSiteTarget? = null
        return annotationNode.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                ANNOTATION_TARGET -> annotationTarget = convertAnnotationTarget(node)
                ANNOTATION_ENTRY -> container += convertUnescapedAnnotation(node, annotationTarget)
            }
        }
    }

    private fun convertAnnotationTarget(annotationUseSiteTarget: LighterASTNode): AnnotationUseSiteTarget {
        var annotationTarget: AnnotationUseSiteTarget? = null
        annotationUseSiteTarget.forEachChildren {
            when (it.tokenType) {
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

        //TODO specify error
        return annotationTarget ?: throw Exception()
    }

    //equals to ANNOTATION_ENTRY
    private fun convertUnescapedAnnotation(
        unescapedAnnotation: LighterASTNode,
        annotationUseSiteTarget: AnnotationUseSiteTarget?
    ): FirAnnotationCall {
        //TODO not implemented
        val pair = convertConstructorInvocation(unescapedAnnotation)
        return FirAnnotationCallImpl(
            session,
            null,
            annotationUseSiteTarget,
            pair.first
        ).apply {
            arguments += pair.second
        }
    }

    private fun visitExpression(expression: LighterASTNode): FirExpression {
        return if (stubMode) FirExpressionStub(session, null)
        else TODO("not implemented")
    }

    private fun visitBlock(block: LighterASTNode?): FirBlock {
        return if (!stubMode) {
            TODO("not implemented")
            //visitStatements(ctx.statements())
        } else {
            FirSingleExpressionBlock(
                session,
                FirExpressionStub(session, null).toReturn()
            )
        }
    }
}