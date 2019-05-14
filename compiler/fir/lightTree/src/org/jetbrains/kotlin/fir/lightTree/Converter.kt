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
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.toFirProperty
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.toReturn
import org.jetbrains.kotlin.fir.lightTree.ConverterUtil.toFirExpression
import org.jetbrains.kotlin.fir.lightTree.FunctionUtil.removeLast
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtDotQualifiedExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStringTemplateExpressionElementType

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
    private val implicitType = FirImplicitTypeRefImpl(session, null)

    private inline fun LighterASTNode.forEachChildren(f: (LighterASTNode) -> Unit) {
        val kidsRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, kidsRef)
        val kidsArray = kidsRef.get()
        for (kid in kidsArray) {
            if (kid == null) continue
            val tokenType = kid.tokenType
            if (COMMENTS.contains(tokenType) || tokenType == WHITE_SPACE || tokenType == SEMICOLON || tokenType == COLON) continue
            f(kid)
        }
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
                FILE_ANNOTATION_LIST -> fileAnnotationList += convertFileAnnotation(it)
                PACKAGE_DIRECTIVE -> ClassNameUtil.packageFqName = convertPackageHeader(it)
                IMPORT_LIST -> importList += convertImportList(it)
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

    private fun convertFileAnnotation(fileAnnotationList: LighterASTNode): List<FirAnnotationCall> {
        val annotationList = mutableListOf<FirAnnotationCall>()
        fileAnnotationList.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> annotationList += convertAnnotation(it)
            }
        }

        return annotationList
    }

    private fun convertPackageHeader(packageNode: LighterASTNode): FqName {
        var packageName: FqName = FqName.ROOT
        packageNode.forEachChildren {
            when (it.tokenType) {
                DOT_QUALIFIED_EXPRESSION, REFERENCE_EXPRESSION -> packageName = FqName(it.toString())
            }
        }
        return packageName
    }

    private fun convertImportList(importList: LighterASTNode): List<FirImport> {
        val imports = mutableListOf<FirImport>()
        importList.forEachChildren {
            when (it.tokenType) {
                IMPORT_DIRECTIVE -> imports += convertImportDirective(it)
            }
        }

        return imports
    }

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
            aliasName?.let { Name.identifier(it) }
        )
    }

    private fun convertImportAlias(importAlias: LighterASTNode): String {
        var alias: String? = null
        importAlias.forEachChildren {
            when (it.tokenType) {
                IDENTIFIER -> alias = it.toString()
            }
        }

        //TODO specify error
        return alias ?: throw Exception()
    }

    private fun convertClass(classNode: LighterASTNode): FirDeclaration {
        var modifiers = Modifier(session)
        var classKind: ClassKind? = null
        var identifier: String? = null
        val typeParameters = mutableListOf<FirTypeParameter>()
        var primaryConstructor: LighterASTNode? = null
        val superTypeRefs = mutableListOf<FirTypeRef>()
        val superTypeCallEntry = mutableListOf<FirExpression>()
        var delegatedSuperTypeRef: FirTypeRef? = null
        val typeConstraints = mutableListOf<Pair<String, FirTypeRef>>()
        var classBody: LighterASTNode? = null
        classNode.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                CLASS_KEYWORD -> classKind = ClassKind.CLASS
                INTERFACE_KEYWORD -> classKind = ClassKind.INTERFACE
                OBJECT_KEYWORD -> classKind = ClassKind.OBJECT
                IDENTIFIER -> identifier = it.toString()
                TYPE_PARAMETER_LIST -> typeParameters += convertTypeParameters(it)
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

        classKind = when {
            modifiers.classModifier == ClassModifier.ENUM -> ClassKind.ENUM_CLASS
            modifiers.classModifier == ClassModifier.ANNOTATION -> ClassKind.ANNOTATION_CLASS
            else -> classKind!!
        }
        val className = identifier.nameAsSafeName(if (modifiers.classModifier == ClassModifier.COMPANION) "Companion" else "")

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
                classKind!!,
                isInner = modifiers.classModifier == ClassModifier.INNER,
                isCompanion = modifiers.classModifier == ClassModifier.COMPANION,
                isData = modifiers.classModifier == ClassModifier.DATA,
                isInline = modifiers.functionModifier == FunctionModifier.INLINE
            )
            firClass.annotations += modifiers.annotations
            firClass.typeParameters += typeParameters
            typeConstraints.forEach { (identifier, type) ->
                firClass.typeParameters.forEach { typeParameter ->
                    if (identifier == typeParameter.name.identifier) {
                        (typeParameter as FirTypeParameterImpl).bounds += type
                        typeParameter.annotations += type.annotations
                    }
                }
            }
            firClass.superTypeRefs += superTypeRefs

            var firPrimaryConstructor: FirConstructor? = null
            val hasSecondaryConstructor = classBody.hasSecondaryConstructor(tree)
            val delegatedSelfType = identifier.nameAsSafeName().toDelegatedSelfType(session)
            if (classKind != ClassKind.INTERFACE) {
                delegatedSuperTypeRef =
                    delegatedSuperTypeRef ?: if (classKind == ClassKind.ENUM_CLASS) implicitEnumType else implicitAnyType
                firPrimaryConstructor = convertPrimaryConstructor(
                    primaryConstructor,
                    hasSecondaryConstructor,
                    superTypeCallEntry,
                    delegatedSelfType,
                    delegatedSuperTypeRef!!
                )?.apply {
                    firClass.declarations += this
                }
            }
            getValueParameters(primaryConstructor).forEach {
                if (it.isVal || it.isVar) {
                    firClass.declarations += it.toFirProperty()
                }
            }

            if (classKind == ClassKind.ENUM_CLASS) {
                // separate converter only for enum entries
                classBody?.let {
                    val firEnumEntries = convertEnumClassBody(it)
                    firEnumEntries.forEach { firEnumEntry ->
                        val enumDelegatedSelfTypeRef = className.toDelegatedSelfType(session)
                        val enumDelegatedSuperTypeRef: FirTypeRef?
                        val enumSuperTypeCallEntry = mutableListOf<FirValueParameter>()

                        if (firPrimaryConstructor != null && firPrimaryConstructor.valueParameters.isNotEmpty()) {
                            enumDelegatedSuperTypeRef = enumDelegatedSelfTypeRef
                            firEnumEntry.superTypeRefs += enumDelegatedSuperTypeRef
                            enumSuperTypeCallEntry += firPrimaryConstructor.valueParameters
                        } else {
                            enumDelegatedSuperTypeRef = implicitAnyType
                        }

                        val firEnumPrimaryConstructor = convertPrimaryConstructor(
                            null,  //null means that enum entry has no primary consctuctor
                            false,
                            firPrimaryConstructor?.valueParameters?.map { valueParameter ->
                                valueParameter.toFirExpression(
                                    session,
                                    stubMode
                                )
                            } ?: listOf(),
                            enumDelegatedSelfTypeRef,
                            enumDelegatedSuperTypeRef
                        )
                        firEnumEntry.declarations.add(0, firEnumPrimaryConstructor!!)
                    }
                    firClass.declarations += firEnumEntries
                }
            }
            classBody?.let { firClass.declarations += convertClassBody(it) }
            if (hasSecondaryConstructor) {
                firClass.declarations.stream().filter { it is FirConstructor }.forEach { secondaryConstructor ->
                    (secondaryConstructor as FirConstructorImpl).returnTypeRef = delegatedSelfType
                    val constructorDelegationCall = secondaryConstructor.delegatedConstructor as? FirDelegatedConstructorCallImpl
                    val isThis =
                        (constructorDelegationCall == null && primaryConstructor != null) || constructorDelegationCall?.isThis == true
                    val delegatedType = when {
                        isThis -> delegatedSelfType
                        else -> delegatedSuperTypeRef ?: FirErrorTypeRefImpl(session, null, "No super type")
                    }
                    constructorDelegationCall?.constructedTypeRef = delegatedType
                }
            }

            return@withChildClassName firClass
        }
    }

    private fun getValueParameters(primaryConstructor: LighterASTNode?): List<ValueParameter> {
        if (primaryConstructor == null) return listOf()

        val valueParameters = mutableListOf<ValueParameter>()
        primaryConstructor.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER_LIST -> valueParameters += convertClassParameters(it)
            }
        }

        return valueParameters
    }

    private fun convertPrimaryConstructor(
        primaryConstructor: LighterASTNode?,
        hasSecondaryConstructor: Boolean,
        superTypeCallEntry: List<FirExpression>,
        delegatedSelfType: FirTypeRef,
        delegatedSuperTypeRef: FirTypeRef
    ): FirConstructor? {
        if (primaryConstructor == null && hasSecondaryConstructor) return null

        var modifiers = Modifier(session)
        val valueParameters = mutableListOf<ValueParameter>()
        primaryConstructor?.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                VALUE_PARAMETER_LIST -> valueParameters += convertClassParameters(it)
            }
        }

        val firDelegatedCall = FirDelegatedConstructorCallImpl(
            session,
            null,
            delegatedSuperTypeRef,
            isThis = false
        )
        return FirPrimaryConstructorImpl(
            session,
            null,
            FirFunctionSymbol(ClassNameUtil.callableIdForClassConstructor()),
            modifiers.visibilityModifier.toVisibility(),
            modifiers.platformModifier == PlatformModifier.EXPECT,
            modifiers.platformModifier == PlatformModifier.ACTUAL,
            delegatedSelfType,
            firDelegatedCall
        ).apply {
            annotations += modifiers.annotations
            this.valueParameters += valueParameters.map { it.firValueParameter }
            delegatedConstructor?.apply {
                if (!stubMode) {
                    TODO("not implemented")
                    //superTypeCallEntry?.extractArgumentsTo(this)
                }
            }
        }
    }

    private fun convertClassParameters(classParameters: LighterASTNode): List<ValueParameter> {
        val valueParameters = mutableListOf<ValueParameter>()
        classParameters.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER -> valueParameters += convertClassParameter(it)
            }
        }

        return valueParameters
    }

    private fun convertClassParameter(classParameter: LighterASTNode): ValueParameter {
        var modifiers = Modifier(session)
        var isVal = false
        var isVar = false
        var identifier: String? = null
        var firType: FirTypeRef? = null
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
            Name.identifier(identifier!!),
            firType!!,
            firExpression,
            isCrossinline = modifiers.parameterModifier == ParameterModifier.CROSSINLINE,
            isNoinline = modifiers.parameterModifier == ParameterModifier.NOINLINE,
            isVararg = modifiers.parameterModifier == ParameterModifier.VARARG
        ).apply { annotations += modifiers.annotations }
        return ValueParameter(isVal, isVar, modifiers, firValueParameter)
    }

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

    /*private fun convertDelegationSpecifier(delegationSpecifier: LighterASTNode): FirElement {

    }*/

    private fun convertConstructorInvocation(constructorInvocation: LighterASTNode): Pair<FirTypeRef, List<FirExpression>> {
        var firTypeRef: FirTypeRef? = null
        val firValueArguments = mutableListOf<FirExpression>()
        constructorInvocation.forEachChildren {
            when (it.tokenType) {
                CONSTRUCTOR_CALLEE -> firTypeRef = convertType(it)
                VALUE_ARGUMENT_LIST -> if (!stubMode) firValueArguments += convertValueArguments(it)
            }
        }
        return Pair(firTypeRef!!, firValueArguments)
    }

    private fun convertExplicitDelegation(explicitDelegation: LighterASTNode): FirDelegatedTypeRef {
        var firTypeRef: FirTypeRef? = null
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
            firTypeRef!!,
            expression
        )
    }

    private fun convertClassBody(classBody: LighterASTNode): List<FirDeclaration> {
        val firDeclarationList = mutableListOf<FirDeclaration>()
        classBody.forEachChildren {
            when (it.tokenType) {
                CLASS -> firDeclarationList += convertClass(it)
                FUN -> firDeclarationList += convertFunctionDeclaration(it)
                PROPERTY -> firDeclarationList += convertPropertyDeclaration(it)
                TYPEALIAS -> firDeclarationList += convertTypeAlias(it)
                OBJECT_DECLARATION -> firDeclarationList += convertClass(it)
                CLASS_INITIALIZER -> firDeclarationList += convertAnonymousInitializer(it) //anonymousInitializer
                SECONDARY_CONSTRUCTOR -> firDeclarationList += convertSecondaryConstructor(it)
            }
        }

        return firDeclarationList
    }

    private fun convertConstructorDelegationCall(constructorDelegationCall: LighterASTNode): FirDelegatedConstructorCall {
        var isThis: Boolean = false
        var isSuper: Boolean = false
        constructorDelegationCall.forEachChildren {
            when (it.tokenType) {
                CONSTRUCTOR_DELEGATION_REFERENCE -> {
                    if (it.toString() == "this") isThis = true
                    if (it.toString() == "super") isSuper = true
                }
                VALUE_ARGUMENT_LIST -> "" //TODO implement
            }
        }

        /*val isThis = this?.THIS() != null || (this == null && hasPrimaryConstructor)
        val delegatedType = when {
            isThis -> delegatedSelfTypeRef
            else -> delegatedSuperTypeRef ?: FirErrorTypeRefImpl(session, null, "No super type")
        }*/
        return FirDelegatedConstructorCallImpl(
            session,
            null,
            implicitType,
            isThis
        ).apply {
            if (!stubMode) {
                TODO("not implemented")
                //extractArgumentsTo(this)
            }
        }
    }

    private fun convertEnumClassBody(classBody: LighterASTNode): List<FirEnumEntryImpl> {
        val firDeclarationList = mutableListOf<FirEnumEntryImpl>()
        classBody.forEachChildren {
            when (it.tokenType) {
                ENUM_ENTRY -> firDeclarationList += convertEnumEntry(it)
            }
        }

        return firDeclarationList
    }

    /*private fun convertClassMemberDeclarations(classMemberDeclarations: LighterASTNode): FirElement {

    }*/

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
            implicitType,//delegatedSelfTypeRef,
            constructorDelegationCall//this?.constructorDelegationCall().convert(delegatedSuperTypeRef, delegatedSelfTypeRef, hasPrimaryConstructor)
        )
        FunctionUtil.firFunctions += firConstructor
        firConstructor.annotations += modifiers.annotations
        firConstructor.valueParameters += firValueParameters
        firConstructor.body = visitFunctionBody(firBlock, null)
        FunctionUtil.firFunctions.removeLast()
        return firConstructor
    }

    private fun convertEnumEntry(enumEntry: LighterASTNode): FirEnumEntryImpl {
        var modifiers = Modifier(session)
        var identifier: String? = null
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

        val enumEntryName = Name.identifier(identifier!!)
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

    private fun convertFunctionDeclaration(functionDeclaration: LighterASTNode): FirDeclaration {
        var modifiers = Modifier(session)
        var identifier: String? = null
        val typeParameters = mutableListOf<FirTypeParameter>()
        val valueParametersList = mutableListOf<FirValueParameter>()
        var isReturnType = false
        var receiverType: FirTypeRef? = null
        var returnType: FirTypeRef? = null
        val typeConstraints = mutableListOf<Pair<String, FirTypeRef>>()
        var firBlock: FirBlock? = null
        var firExpression: FirExpression? = null
        functionDeclaration.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_PARAMETER_LIST -> typeParameters += convertTypeParameters(it)
                VALUE_PARAMETER_LIST -> valueParametersList += convertFunctionValueParameters(it)
                COLON -> isReturnType = true
                TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverType = convertType(it)
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                BLOCK -> firBlock = visitBlock(it)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType -> firExpression = visitExpression(it) //TODO implement
            }
        }

        returnType = if (firBlock != null || modifiers.inheritanceModifier == InheritanceModifier.ABSTRACT) {
            returnType ?: implicitUnitType
        } else {
            returnType ?: implicitType
        }
        val functionName = Name.identifier(identifier!!)
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

        firFunction.typeParameters += typeParameters
        typeConstraints.forEach { (identifier, type) ->
            firFunction.typeParameters.forEach { typeParameter ->
                if (identifier == typeParameter.name.identifier) {
                    (typeParameter as FirTypeParameterImpl).bounds += type
                    typeParameter.annotations += type.annotations
                }
            }
        }

        firFunction.valueParameters += valueParametersList
        firFunction.body = visitFunctionBody(firBlock, firExpression)
        FunctionUtil.firFunctions.removeLast()
        return firFunction
    }

    private fun convertFunctionValueParameters(functionValueParameters: LighterASTNode): List<FirValueParameter> {
        val firValueParameters = mutableListOf<FirValueParameter>()
        functionValueParameters.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER -> firValueParameters += convertFunctionValueParameter(it)
            }
        }

        return firValueParameters
    }

    private fun convertFunctionValueParameter(functionValueParameter: LighterASTNode): FirValueParameter {
        var modifiers = Modifier(session)
        var identifier: String? = null
        var firType: FirTypeRef? = null
        var firExpression: FirExpression? = null
        functionValueParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_REFERENCE -> firType = convertType(it)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType -> firExpression = visitExpression(it) //TODO implement
            }
        }

        return FirValueParameterImpl(
            session,
            null,
            Name.identifier(identifier!!),
            firType!!,
            firExpression,
            isCrossinline = modifiers.parameterModifier == ParameterModifier.CROSSINLINE,
            isNoinline = modifiers.parameterModifier == ParameterModifier.NOINLINE,
            isVararg = modifiers.parameterModifier == ParameterModifier.VARARG
        ).apply { annotations += modifiers.annotations }
    }

    private fun convertParameter(parameter: LighterASTNode): FirValueParameter {
        var identifier: String? = null
        var firType: FirTypeRef? = null
        parameter.forEachChildren {
            when (it.tokenType) {
                IDENTIFIER -> identifier = it.toString()
                TYPE_REFERENCE -> firType = convertType(it)
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

    private fun convertSetterParameter(setterParameter: LighterASTNode, propertyTypeRef: FirTypeRef): FirValueParameter {
        var modifiers = Modifier(session)
        var firValueParameter: FirValueParameter? = null
        setterParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                VALUE_PARAMETER -> firValueParameter = convertParameter(it)
            }
        }

        return FirValueParameterImpl(
            session,
            null,
            firValueParameter!!.name,
            if (firValueParameter!!.returnTypeRef == implicitType) propertyTypeRef else firValueParameter!!.returnTypeRef,
            null,
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

    private fun convertPropertyDeclaration(property: LighterASTNode): FirDeclaration {
        //TODO DESTRUCTURING_DECLARATION
        var modifiers = Modifier(session)
        var identifier: String? = null
        val typeParameters = mutableListOf<FirTypeParameter>()
        var isReturnType = false
        var isDelegate = false
        var isVar = false
        var receiverType: FirTypeRef? = null
        var returnType: FirTypeRef = implicitType
        val typeConstraints = mutableListOf<Pair<String, FirTypeRef>>()
        var getter: FirPropertyAccessor? = null
        var setter: FirPropertyAccessor? = null
        var firExpression: FirExpression? = null
        property.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_PARAMETER_LIST -> typeParameters += convertTypeParameters(it)
                COLON -> isReturnType = true
                TYPE_REFERENCE -> if (isReturnType) returnType = convertType(it) else receiverType = convertType(it)
                TYPE_CONSTRAINT_LIST -> typeConstraints += convertTypeConstraints(it)
                BY_KEYWORD -> isDelegate = true
                VAR_KEYWORD -> isVar = true
                PROPERTY_ACCESSOR ->
                    if (it.toString().contains("get")) //TODO make it better
                        getter = convertGetter(it, returnType)
                    else
                        setter = convertSetter(it, returnType)
                is KtNodeType,
                is KtConstantExpressionElementType,
                is KtDotQualifiedExpressionElementType,
                is KtStringTemplateExpressionElementType -> firExpression = visitExpression(it) //TODO implement
            }
        }

        val propertyName = Name.identifier(identifier!!)

        return if (FunctionUtil.firFunctions.isNotEmpty()) {
            FirVariableImpl(
                session,
                null,
                propertyName,
                returnType,
                isVar,
                firExpression,
                if (isDelegate) {
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
                this.typeParameters += typeParameters
                typeConstraints.forEach { (identifier, type) ->
                    this.typeParameters.forEach { typeParameter ->
                        if (identifier == typeParameter.name.identifier) {
                            (typeParameter as FirTypeParameterImpl).bounds += type
                            typeParameter.annotations += type.annotations
                        }
                    }
                }
                annotations += modifiers.annotations
            }
        }
    }

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
                is KtStringTemplateExpressionElementType -> firExpression = visitExpression(it) //TODO implement
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
                is KtStringTemplateExpressionElementType -> firExpression = visitExpression(it) //TODO implement
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

        firAccessor.valueParameters += if (firValueParameters.isEmpty()) listOf(
            FirDefaultSetterValueParameter(
                session,
                null,
                propertyTypeRef
            )
        ) else firValueParameters

        firAccessor.body = visitFunctionBody(firBlock, firExpression)
        FunctionUtil.firFunctions.removeLast()
        return firAccessor
    }

    private fun convertTypeAlias(typeAlias: LighterASTNode): FirDeclaration {
        var modifiers = Modifier(session)
        var identifier: String? = null
        var firType: FirTypeRef? = null
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        typeAlias.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertModifiers(it)
                IDENTIFIER -> identifier = it.toString()
                TYPE_PARAMETER_LIST -> firTypeParameters += convertTypeParameters(it)
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        val typeAliasName = Name.identifier(identifier!!)
        return ClassNameUtil.withChildClassName(typeAliasName) {
            return@withChildClassName FirTypeAliasImpl(
                session,
                null,
                FirTypeAliasSymbol(ClassNameUtil.currentClassId),
                typeAliasName,
                modifiers.visibilityModifier.toVisibility(),
                modifiers.platformModifier == PlatformModifier.EXPECT,
                modifiers.platformModifier == PlatformModifier.ACTUAL,
                firType!!
            ).apply {
                annotations += modifiers.annotations
                typeParameters += firTypeParameters
            }
        }
    }

    private fun convertTypeParameters(typeParameterList: LighterASTNode): List<FirTypeParameter> {
        val firTypeParameters = mutableListOf<FirTypeParameter>()
        typeParameterList.forEachChildren {
            when (it.tokenType) {
                TYPE_PARAMETER -> firTypeParameters += convertTypeParameter(it)
            }
        }

        return firTypeParameters
    }

    private fun convertTypeParameter(typeParameter: LighterASTNode): FirTypeParameter {
        var typeParameterModifiers = TypeParameterModifier(session)
        var parameterName: String? = null
        var type: FirTypeRef? = null
        typeParameter.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> typeParameterModifiers = convertTypeParameterModifiers(it)
                IDENTIFIER -> parameterName = it.toString()
                TYPE_REFERENCE -> type = convertType(it)
            }
        }

        val firTypeParameter = FirTypeParameterImpl(
            session,
            null,
            FirTypeParameterSymbol(),
            Name.identifier(parameterName!!),
            typeParameterModifiers.varianceModifier.toVariance(),
            typeParameterModifiers.reificationModifier != null
        )
        firTypeParameter.annotations += typeParameterModifiers.annotations
        type?.let { firTypeParameter.bounds += it }

        return firTypeParameter
    }

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

    /*fun convertTypeParameterModifier(typeParameterModifier: LighterASTNode): TypeParameterModifier {

    }*/

    private fun convertType(type: LighterASTNode): FirTypeRef {
        var typeModifiers = TypeModifier(session) //TODO what with suspend?
        var firType: FirTypeRef? = null
        type.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firType = convertType(it)
                MODIFIER_LIST -> typeModifiers = convertTypeModifiers(it)
                USER_TYPE -> firType = convertUserType(it).also { (it as FirUserTypeRefImpl).qualifier.reverse() }
                NULLABLE_TYPE -> firType = convertNullableType(it)
                FUNCTION_TYPE -> firType = convertFunctionType(it)
                DYNAMIC_TYPE -> firType = FirDynamicTypeRefImpl(session, null, false)
            }
        }
        //TODO specify error - unknown type
        return firType?.also { (it as FirAbstractAnnotatedTypeRef).annotations += typeModifiers.annotations } ?: throw Exception()
    }

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

    /*private fun convertTypeModifier(typeModifier: LighterASTNode): TypeModifier {

    }*/

    private fun convertNullableType(nullableType: LighterASTNode): FirTypeRef {
        var firTypeRef: FirTypeRef? = null
        nullableType.forEachChildren {
            when (it.tokenType) {
                USER_TYPE -> firTypeRef = convertUserType(it, true)
                FUNCTION_TYPE -> firTypeRef = convertFunctionType(it, true)
                NULLABLE_TYPE -> firTypeRef = convertNullableType(it)
                DYNAMIC_TYPE -> firTypeRef = FirDynamicTypeRefImpl(session, null, true)
            }
        }

        //TODO specify error
        return firTypeRef ?: throw Exception()
    }

    private fun convertFunctionType(functionType: LighterASTNode, isNullable: Boolean = false): FirTypeRef {
        var receiverTypeReference: FirTypeRef? = null
        var returnTypeReference: FirTypeRef? = null
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
            returnTypeReference!!
        ).apply { valueParameters += valueParametersList }
    }

    private fun convertReceiverType(receiverType: LighterASTNode): FirTypeRef? {
        var firTypeRef: FirTypeRef? = null
        receiverType.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firTypeRef = convertType(it)
            }
        }

        //TODO specify error
        return firTypeRef ?: throw Exception()
    }

    private fun convertUserType(userType: LighterASTNode, isNullable: Boolean = false): FirUserTypeRef {
        var simpleFirUserType: FirUserTypeRef? = null
        var identifier: String? = null
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        userType.forEachChildren {
            when (it.tokenType) {
                USER_TYPE -> simpleFirUserType = convertUserType(it) //simple user type
                REFERENCE_EXPRESSION -> identifier = it.toString()
                TYPE_ARGUMENT_LIST -> firTypeArguments += convertTypeArguments(it)
            }
        }

        val qualifier = FirQualifierPartImpl(
            Name.identifier(identifier!!)
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

    private fun convertFunctionTypeParameters(functionTypeParameters: LighterASTNode): List<FirValueParameter> {
        val valueParametersList = mutableListOf<FirValueParameter>()
        functionTypeParameters.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER -> valueParametersList += convertParameter(it)
            }
        }

        return valueParametersList
    }

    private fun convertTypeConstraints(typeConstraints: LighterASTNode): List<Pair<String, FirTypeRef>> {
        val typeConstraintsList = mutableListOf<Pair<String, FirTypeRef>>()
        typeConstraints.forEachChildren {
            when (it.tokenType) {
                TYPE_CONSTRAINT -> typeConstraintsList += convertTypeConstraint(it)
            }
        }

        return typeConstraintsList
    }

    private fun convertTypeConstraint(typeConstraint: LighterASTNode): Pair<String, FirTypeRef> {
        var identifier: String? = null
        var firType: FirTypeRef? = null
        typeConstraint.forEachChildren {
            when (it.tokenType) {
                REFERENCE_EXPRESSION -> identifier = it.toString()
                TYPE_REFERENCE -> firType = convertType(it)
            }
        }

        return Pair(identifier!!, firType!!)
    }

    private fun convertValueArguments(valueArguments: LighterASTNode): List<FirExpression> {
        val firValueArguments = mutableListOf<FirExpression>()
        valueArguments.forEachChildren {
            when (it.tokenType) {
                VALUE_ARGUMENT -> firValueArguments += convertValueArgument(it)
            }
        }
        return firValueArguments
    }

    private fun convertTypeArguments(typeArguments: LighterASTNode): List<FirTypeProjection> {
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        typeArguments.forEachChildren {
            when (it.tokenType) {
                TYPE_PROJECTION -> firTypeArguments += convertTypeProjection(it)
            }
        }

        return firTypeArguments
    }

    private fun convertTypeProjection(typeProjection: LighterASTNode): FirTypeProjection {
        var modifiers = TypeProjectionModifier(session)
        var type: FirTypeRef? = null
        var isStarProjection = false
        typeProjection.forEachChildren {
            when (it.tokenType) {
                MODIFIER_LIST -> modifiers = convertTypeProjectionModifiers(it)
                TYPE_REFERENCE -> type = convertType(it)
                MUL -> isStarProjection = true
            }
        }

        //TODO what with annotations?
        return if (isStarProjection) FirStarProjectionImpl(session, null)
        else FirTypeProjectionWithVarianceImpl(
            session,
            null,
            modifiers.varianceModifier.toVariance(),
            type!!
        )
    }

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

    /*private fun convertTypeProjectionModifier(modifier: LighterASTNode): TypeProjectionModifier {

    }*/

    private fun convertValueArgument(valueArgument: LighterASTNode): FirExpression {
        return FirErrorExpressionImpl(session, null, "Not implemented")
        //TODO implement
    }

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

    /*fun convertModifier(modifier: LighterASTNode): Modifier {

    }*/

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
        val annotationList = mutableListOf<FirAnnotationCall>()
        annotationNode.forEachChildren {
            when (it.tokenType) {
                ANNOTATION_TARGET -> annotationTarget = convertAnnotationTarget(it)
                ANNOTATION_ENTRY -> annotationList += convertUnescapedAnnotation(it, annotationTarget)
            }
        }

        return annotationList
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
        return FirAnnotationCallImpl(
            session,
            null,
            annotationUseSiteTarget,
            FirErrorTypeRefImpl(session, null, "not implemented") //TODO not implemented
        )
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