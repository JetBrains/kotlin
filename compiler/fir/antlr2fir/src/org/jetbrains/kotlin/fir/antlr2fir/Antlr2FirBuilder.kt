/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.antlr2fir

import org.antlr.v4.runtime.*
import org.jetbrains.kotlin.fir.antlr2fir.antlr4.generated.KotlinParserBaseVisitor
import org.jetbrains.kotlin.fir.antlr2fir.antlr4.generated.KotlinParser
import org.jetbrains.kotlin.fir.antlr2fir.fir.AnnotationContainer
import org.jetbrains.kotlin.fir.antlr2fir.fir.modifier.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

class Antlr2FirBuilder(val session: FirSession, val stubMode: Boolean, val fileName: String = "") {

    private val implicitUnitType = FirImplicitUnitTypeRef(session, null)

    private val implicitAnyType = FirImplicitAnyTypeRef(session, null)

    private val implicitEnumType = FirImplicitEnumTypeRef(session, null)

    private val implicitType = FirImplicitTypeRefImpl(session, null)

    fun buildFirFile(file: KotlinParser.KotlinFileContext): FirFile {
        return file.accept(Visitor()) as FirFile
    }

    inner class Visitor : KotlinParserBaseVisitor<FirElement>() {
        private inline fun <reified R : FirElement> ParserRuleContext?.convertSafe(): R? =
            this?.accept(this@Visitor) as? R

        private inline fun <reified R : FirElement> ParserRuleContext.convert(): R =
            this.accept(this@Visitor) as R

        private fun KotlinParser.TypeContext?.toFirOrImplicitType(): FirTypeRef =
            convertSafe() ?: implicitType

        private fun KotlinParser.TypeContext?.toFirOrUnitType(): FirTypeRef =
            convertSafe() ?: implicitUnitType

        private fun KotlinParser.TypeContext?.toFirOrErrorType(): FirTypeRef =
            convertSafe() ?: FirErrorTypeRefImpl(session, null, if (this == null) "Incomplete code" else "Conversion failed")

        private fun FirExpression.toReturn(labelName: String? = null): FirReturnExpression {
            return FirReturnExpressionImpl(
                session,
                null,
                this
            ).apply {
                target = FirFunctionTarget(labelName)
                val lastFunction = firFunctions.lastOrNull()
                if (labelName == null) {
                    if (lastFunction != null) {
                        target.bind(lastFunction)
                    } else {
                        target.bind(FirErrorFunction(session, psi, "Cannot bind unlabeled return to a function"))
                    }
                } else {
                    for (firFunction in firFunctions.asReversed()) {
                        when (firFunction) {
                            is FirAnonymousFunction -> {
                                if (firFunction.label?.name == labelName) {
                                    target.bind(firFunction)
                                    return@apply
                                }
                            }
                            is FirNamedFunction -> {
                                if (firFunction.name.asString() == labelName) {
                                    target.bind(firFunction)
                                    return@apply
                                }
                            }
                        }
                    }
                    target.bind(FirErrorFunction(session, psi, "Cannot bind label $labelName to a function"))
                }
            }
        }

        private val firFunctions = mutableListOf<FirFunction>()

        private fun <T> MutableList<T>.removeLast() {
            removeAt(size - 1)
        }

        private fun <T> MutableList<T>.pop(): T? {
            val result = lastOrNull()
            if (result != null) {
                removeAt(size - 1)
            }
            return result
        }

        lateinit var packageFqName: FqName

        override fun visitKotlinFile(ctx: KotlinParser.KotlinFileContext): FirFileImpl {
            packageFqName = ctx.packageHeader()?.let { FqName(it.identifier().text) } ?: FqName.ROOT
            val firFile = FirFileImpl(session, null, fileName, packageFqName)

            for (fileAnnotation in ctx.fileAnnotation()) {
                firFile.annotations += visitFileAnnotation(fileAnnotation).annotations
            }
            for (importDirective in ctx.importList().importHeader()) {
                firFile.imports += visitImportHeader(importDirective)
            }
            for (topLevelObject in ctx.topLevelObject()) {
                firFile.declarations += topLevelObject.declaration().convert<FirDeclaration>()
            }
            return firFile
        }

        override fun visitScript(ctx: KotlinParser.ScriptContext): FirElement {
            TODO("not implemented")
        }

        override fun visitFileAnnotation(ctx: KotlinParser.FileAnnotationContext): AnnotationContainer {
            val container = mutableListOf<FirAnnotationCall>()
            for (annotationEntry in ctx.unescapedAnnotation()) {
                val firAnnotationCall = annotationEntry.convert<FirAnnotationCall>()
                container += FirAnnotationCallImpl(
                    session,
                    null,
                    AnnotationUseSiteTarget.FILE,
                    firAnnotationCall.annotationTypeRef
                )
            }

            return AnnotationContainer(
                session,
                container
            )
        }

        override fun visitPackageHeader(ctx: KotlinParser.PackageHeaderContext?): FirElement {
            TODO("not used")
        }

        override fun visitImportList(ctx: KotlinParser.ImportListContext?): FirElement {
            TODO("not used")
        }

        override fun visitImportHeader(ctx: KotlinParser.ImportHeaderContext): FirImport {
            return FirImportImpl(
                session,
                null,
                FqName(ctx.identifier().text),
                ctx.text.contains("*"),
                ctx.importAlias()?.text?.let { Name.identifier(it) }
            )
        }

        override fun visitImportAlias(ctx: KotlinParser.ImportAliasContext?): FirElement {
            TODO("not used")
        }

        private inline fun <T> withChildClassName(name: Name, l: () -> T): T {
            className = className.child(name)
            val t = l()
            className = className.parent()
            return t
        }

        val currentClassId
            get() = ClassId(packageFqName, className, false)

        fun callableIdForName(name: Name) =
            if (className == FqName.ROOT) CallableId(packageFqName, name)
            else CallableId(packageFqName, className, name)

        fun callableIdForClassConstructor() =
            if (className == FqName.ROOT) CallableId(packageFqName, Name.special("<anonymous-init>"))
            else CallableId(packageFqName, className, className.shortName())

        var className: FqName = FqName.ROOT

        override fun visitTopLevelObject(ctx: KotlinParser.TopLevelObjectContext?): FirElement {
            TODO("not used")
        }

        private fun visitClass(
            name: Name,
            classKind: ClassKind,
            modifier: Modifier,
            typeParameters: List<KotlinParser.TypeParameterContext>?,
            typeConstraints: List<KotlinParser.TypeConstraintContext>?,
            primaryConstructor: KotlinParser.PrimaryConstructorContext?,
            delegationSpecifiers: KotlinParser.DelegationSpecifiersContext?,
            classBody: KotlinParser.ClassBodyContext?,
            enumClassBody: KotlinParser.EnumClassBodyContext?,
            isCompanion: Boolean = false
        ): FirClass {
            return withChildClassName(name) {
                val firClass = FirClassImpl(
                    session,
                    null,
                    FirClassSymbol(currentClassId),
                    name,
                    modifier.visibilityModifier.toVisibility(),
                    modifier.inheritanceModifier?.toModality(),
                    modifier.platformModifier == PlatformModifier.EXPECT,
                    modifier.platformModifier == PlatformModifier.ACTUAL,
                    classKind,
                    isInner = modifier.classModifier == ClassModifier.INNER,
                    isCompanion = isCompanion,
                    isData = modifier.classModifier == ClassModifier.DATA,
                    isInline = modifier.functionModifier == FunctionModifier.INLINE
                )
                firClass.annotations += modifier.annotations
                firClass.typeParameters += typeParameters?.map(this::visitTypeParameter) ?: listOf()
                firClass.typeParameters.forEach { typeParameter ->
                    typeConstraints?.forEach { typeConstraint ->
                        if (typeConstraint.simpleIdentifier().text == typeParameter.name.identifier) {
                            (typeParameter as FirTypeParameterImpl).bounds += visitTypeConstraint(typeConstraint)
                            typeParameter.annotations += typeConstraint.annotation().map(this::visitAnnotation).map { it.annotations }
                                .flatten()
                        }
                    }
                }

                val hasSecondaryConstructor =
                    (classBody != null && classBody.classMemberDeclarations().classMemberDeclaration().any { it.secondaryConstructor() != null }) ||
                            (enumClassBody != null && enumClassBody.classMemberDeclarations()?.classMemberDeclaration()?.any { it.secondaryConstructor() != null } == true)
                val delegatedSelfType = name.toDelegatedSelfType()
                val delegatedSuperType = delegationSpecifiers.extractSuperTypeListEntriesTo(
                    firClass,
                    delegatedSelfType,
                    primaryConstructor,
                    hasSecondaryConstructor = hasSecondaryConstructor,
                    isInterface = classKind == ClassKind.INTERFACE,
                    isEnum = classKind == ClassKind.ENUM_CLASS
                )

                primaryConstructor?.classParameters()?.classParameter()?.forEach {
                    if (it.VAL() != null || it.VAR() != null) {
                        firClass.declarations += it.toFirProperty()
                    }
                }

                var classMemberDeclarations: KotlinParser.ClassMemberDeclarationsContext? = null
                if (enumClassBody != null) {
                    enumClassBody.enumEntries()?.enumEntry()?.forEach {
                        firClass.declarations += it.toFirEnumEntry(
                            primaryConstructor,
                            delegatedSelfType
                        )
                    }
                    classMemberDeclarations = enumClassBody.classMemberDeclarations()
                } else if (classBody != null) {
                    classMemberDeclarations = classBody.classMemberDeclarations()
                }
                classMemberDeclarations?.classMemberDeclaration()?.forEach { declaration ->
                    firClass.declarations += when {
                        declaration.secondaryConstructor() != null -> listOf(
                            declaration.secondaryConstructor().toFirConstructor(
                                delegatedSuperType,
                                delegatedSelfType,
                                primaryConstructor != null
                            )
                        )
                        else -> declaration.visitClassMemberDeclaration()//visitClassMemberDeclaration(declaration)
                    }
                }

                return firClass
            }
        }

        override fun visitClassDeclaration(ctx: KotlinParser.ClassDeclarationContext): FirClass {
            val modifier = visitModifiers(ctx.modifiers())
            val classKind = when {
                modifier.classModifier == ClassModifier.ENUM -> ClassKind.ENUM_CLASS
                modifier.classModifier == ClassModifier.ANNOTATION -> ClassKind.ANNOTATION_CLASS
                ctx.CLASS() != null -> ClassKind.CLASS
                ctx.INTERFACE() != null -> ClassKind.INTERFACE
                else -> throw AssertionError("Unexpected class or object")
            }

            return visitClass(
                ctx.simpleIdentifier().nameAsSafeName(),
                classKind,
                modifier,
                ctx.typeParameters()?.typeParameter(),
                ctx.typeConstraints()?.typeConstraint(),
                ctx.primaryConstructor(),
                ctx.delegationSpecifiers(),
                ctx.classBody(),
                ctx.enumClassBody(),
                false
            )
        }

        private fun KotlinParser.PrimaryConstructorContext?.toFirConstructor(
            superTypeCallEntry: KotlinParser.ConstructorInvocationContext?,
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef
        ): FirConstructor {
            return baseForFirConstructor(delegatedSuperTypeRef, delegatedSelfTypeRef).apply {
                delegatedConstructor?.apply {
                    if (!stubMode) {
                        TODO("not implemented")
                        //superTypeCallEntry?.extractArgumentsTo(this)
                    }
                }
            }
        }

        private fun KotlinParser.PrimaryConstructorContext?.toFirConstructor(
            superTypeCallEntry: KotlinParser.ClassParametersContext?,
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef
        ): FirConstructor {
            return this.baseForFirConstructor(delegatedSuperTypeRef, delegatedSelfTypeRef).apply {
                delegatedConstructor?.apply {
                    if (!stubMode) {
                        TODO("not implemented")
                        //superTypeCallEntry?.extractArgumentsTo(this)
                    }
                }
            }
        }

        private fun KotlinParser.PrimaryConstructorContext?.baseForFirConstructor(
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef
        ): FirConstructor {
            val modifier = visitModifiers(this?.modifiers())
            val firDelegatedCall = FirDelegatedConstructorCallImpl(
                session,
                null,
                delegatedSuperTypeRef,
                isThis = false
            )
            val firConstructor = FirPrimaryConstructorImpl(
                session,
                null,
                FirFunctionSymbol(callableIdForClassConstructor()),
                modifier.visibilityModifier.toVisibility(),
                modifier.platformModifier == PlatformModifier.EXPECT,
                modifier.platformModifier == PlatformModifier.ACTUAL,
                delegatedSelfTypeRef,
                firDelegatedCall
            )
            firConstructor.annotations += modifier.annotations
            firConstructor.valueParameters += this?.classParameters()?.classParameter()?.map(this@Visitor::visitClassParameter) ?: listOf()
            return firConstructor
        }

        override fun visitPrimaryConstructor(ctx: KotlinParser.PrimaryConstructorContext?): FirElement {
            TODO("not used")
        }

        override fun visitClassParameters(ctx: KotlinParser.ClassParametersContext?): FirElement {
            TODO("not used")
        }

        private fun KotlinParser.ClassParameterContext.toFirProperty(): FirDeclaration {
            val modifier = visitModifiers(this.modifiers())
            val name = Name.identifier(this.simpleIdentifier().text)
            val type = visitType(this.type())

            return FirMemberPropertyImpl(
                session,
                null,
                FirPropertySymbol(callableIdForName(name)),
                name,
                modifier.visibilityModifier.toVisibility(),
                modifier.inheritanceModifier?.toModality(),
                modifier.platformModifier == PlatformModifier.EXPECT,
                modifier.platformModifier == PlatformModifier.ACTUAL,
                isOverride = modifier.memberModifier == MemberModifier.OVERRIDE,
                isConst = false,
                isLateInit = false,
                receiverTypeRef = null,
                returnTypeRef = type,
                isVar = VAR() != null,
                initializer = null,
                getter = FirDefaultPropertyGetter(session, null, type, modifier.visibilityModifier.toVisibility()),
                setter = FirDefaultPropertySetter(session, null, type, modifier.visibilityModifier.toVisibility()),
                delegate = null
            ).apply { annotations += modifier.annotations }
        }

        override fun visitClassParameter(ctx: KotlinParser.ClassParameterContext): FirValueParameter {
            val modifier = visitModifiers(ctx.modifiers())
            return FirValueParameterImpl(
                session,
                null,
                Name.identifier(ctx.simpleIdentifier().text),
                visitType(ctx.type()),
                ctx.expression()?.let { visitExpression(it) },
                isCrossinline = modifier.parameterModifier == ParameterModifier.CROSSINLINE,
                isNoinline = modifier.parameterModifier == ParameterModifier.NOINLINE,
                isVararg = modifier.parameterModifier == ParameterModifier.VARARG
            ).apply { annotations += modifier.annotations }
        }

        private fun KotlinParser.DelegationSpecifiersContext?.extractSuperTypeListEntriesTo(
            container: FirModifiableClass,
            delegatedSelfTypeRef: FirTypeRef,
            primaryConstructor: KotlinParser.PrimaryConstructorContext?,
            hasSecondaryConstructor: Boolean = false,
            isEnum: Boolean = false,
            isInterface: Boolean = false
        ): FirTypeRef? {
            var superTypeCallEntry: KotlinParser.ConstructorInvocationContext? = null
            var delegatedSuperTypeRef: FirTypeRef? = null
            val annotationContainer = AnnotationContainer(session, mutableListOf())

            if (this != null) {
                for (superTypeListEntry in annotatedDelegationSpecifier()) {
                    val delegationSpecifier = superTypeListEntry.delegationSpecifier()
                    when (delegationSpecifier.getChild(0)) {
                        is KotlinParser.UserTypeContext -> {
                            container.superTypeRefs += visitUserType(delegationSpecifier.userType())
                        }
                        is KotlinParser.FunctionTypeContext -> {
                            container.superTypeRefs += visitFunctionType(delegationSpecifier.functionType())
                        }
                        is KotlinParser.ConstructorInvocationContext -> {
                            delegatedSuperTypeRef = visitUserType(delegationSpecifier.constructorInvocation().userType())
                            container.superTypeRefs += delegatedSuperTypeRef
                            superTypeCallEntry = delegationSpecifier.constructorInvocation()
                        }
                        is KotlinParser.ExplicitDelegationContext -> {
                            container.superTypeRefs += visitExplicitDelegation(delegationSpecifier.explicitDelegation())
                        }
                    }
                    //TODO where to use?
                    annotationContainer.annotations += superTypeListEntry.annotation().map(this@Visitor::visitAnnotation)
                        .map { it.annotations }.flatten()
                }
            }
            if (isInterface) return delegatedSuperTypeRef

            delegatedSuperTypeRef = delegatedSuperTypeRef ?: (if (isEnum) implicitEnumType else implicitAnyType)
            if (primaryConstructor == null && hasSecondaryConstructor) return delegatedSuperTypeRef

            val firPrimaryConstructor = primaryConstructor.toFirConstructor(
                superTypeCallEntry,
                delegatedSuperTypeRef,
                delegatedSelfTypeRef
            )
            container.declarations += firPrimaryConstructor
            return delegatedSuperTypeRef
        }

        override fun visitDelegationSpecifiers(ctx: KotlinParser.DelegationSpecifiersContext?): FirElement {
            TODO("not used")
        }

        override fun visitAnnotatedDelegationSpecifier(ctx: KotlinParser.AnnotatedDelegationSpecifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitDelegationSpecifier(ctx: KotlinParser.DelegationSpecifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitConstructorInvocation(ctx: KotlinParser.ConstructorInvocationContext?): FirElement {
            TODO("not used")
        }

        override fun visitExplicitDelegation(ctx: KotlinParser.ExplicitDelegationContext): FirDelegatedTypeRef {
            val type = when {
                ctx.userType() != null -> visitUserType(ctx.userType())
                ctx.functionType() != null -> visitFunctionType(ctx.functionType())
                else -> throw AssertionError("Unexpected explicit type element")
            }
            return FirDelegatedTypeRefImpl(
                type,
                FirExpressionStub(session, null)
                //TODO("not implemented")
                //{ ctx.expression() }.toFirExpression("Should have delegate")
            )
        }

        override fun visitClassBody(ctx: KotlinParser.ClassBodyContext?): FirElement {
            TODO("not used")
        }

        override fun visitClassMemberDeclarations(ctx: KotlinParser.ClassMemberDeclarationsContext?): FirElement {
            TODO("not used")
        }

        private fun KotlinParser.ClassMemberDeclarationContext.visitClassMemberDeclaration(): List<FirDeclaration> {
            return when {
                this.declaration() != null -> this.declaration().visitDeclaration()//ctx.declaration().convert<FirDeclaration>()
                this.companionObject() != null -> listOf(this.companionObject().convert<FirDeclaration>())
                this.anonymousInitializer() != null -> listOf(this.anonymousInitializer().convert<FirDeclaration>())
                this.secondaryConstructor() != null -> throw AssertionError("Wrong place to process secondary constructor")
                else -> throw AssertionError("Unexpected declaration")
            }
        }

        override fun visitClassMemberDeclaration(ctx: KotlinParser.ClassMemberDeclarationContext): FirDeclaration {
            return when {
                ctx.declaration() != null -> ctx.declaration().convert<FirDeclaration>()
                ctx.companionObject() != null -> ctx.companionObject().convert<FirDeclaration>()
                ctx.anonymousInitializer() != null -> ctx.anonymousInitializer().convert<FirDeclaration>()
                ctx.secondaryConstructor() != null -> throw AssertionError("Wrong place to process secondary constructor")
                else -> throw AssertionError("Unexpected declaration")
            }
        }

        override fun visitAnonymousInitializer(ctx: KotlinParser.AnonymousInitializerContext): FirDeclaration {
            return FirAnonymousInitializerImpl(
                session,
                null,
                if (stubMode) FirEmptyExpressionBlock(session) else visitBlock(ctx.block())
            )
        }

        private fun KotlinParser.SecondaryConstructorContext?.toFirConstructor(
            delegatedSuperTypeRef: FirTypeRef?,
            delegatedSelfTypeRef: FirTypeRef,
            hasPrimaryConstructor: Boolean
        ): FirConstructor {
            val modifier = visitModifiers(this?.modifiers())
            val firConstructor = FirConstructorImpl(
                session,
                null,
                FirFunctionSymbol(callableIdForClassConstructor()),
                modifier.visibilityModifier.toVisibility(),
                modifier.platformModifier == PlatformModifier.EXPECT,
                modifier.platformModifier == PlatformModifier.ACTUAL,
                delegatedSelfTypeRef,
                this?.constructorDelegationCall().convert(delegatedSuperTypeRef, delegatedSelfTypeRef, hasPrimaryConstructor)
            )
            firFunctions += firConstructor
            firConstructor.annotations += modifier.annotations
            firConstructor.valueParameters += this?.functionValueParameters()?.functionValueParameter()?.map(this@Visitor::visitFunctionValueParameter)
                ?: listOf()
            firConstructor.body = visitBlock(this?.block())
            firFunctions.removeLast()
            return firConstructor
        }

        override fun visitSecondaryConstructor(ctx: KotlinParser.SecondaryConstructorContext?): FirElement {
            TODO("not used")
        }

        private fun KotlinParser.ConstructorDelegationCallContext?.convert(
            delegatedSuperTypeRef: FirTypeRef?,
            delegatedSelfTypeRef: FirTypeRef,
            hasPrimaryConstructor: Boolean
        ): FirDelegatedConstructorCall {
            val isThis = this?.THIS() != null || (this == null && hasPrimaryConstructor)
            val delegatedType = when {
                isThis -> delegatedSelfTypeRef
                else -> delegatedSuperTypeRef ?: FirErrorTypeRefImpl(session, null, "No super type")
            }
            return FirDelegatedConstructorCallImpl(
                session,
                null,
                delegatedType,
                isThis
            ).apply {
                if (!stubMode) {
                    TODO("not implemented")
                    //extractArgumentsTo(this)
                }
            }
        }

        override fun visitConstructorDelegationCall(ctx: KotlinParser.ConstructorDelegationCallContext?): FirElement {
            TODO("not used")
        }

        override fun visitEnumClassBody(ctx: KotlinParser.EnumClassBodyContext?): FirElement {
            TODO("not used")
        }

        override fun visitEnumEntries(ctx: KotlinParser.EnumEntriesContext?): FirElement {
            TODO("not used")
        }

        private fun KotlinParser.EnumEntryContext.toFirEnumEntry(
            primaryConstructor: KotlinParser.PrimaryConstructorContext?,
            baseEnum: FirTypeRef
        ): FirDeclaration {
            val enumEntryName = Name.identifier(this.simpleIdentifier().text)
            val modifier = visitModifiers(this.modifiers())

            return withChildClassName(enumEntryName) {
                val firEnumEntry = FirEnumEntryImpl(
                    session,
                    null,
                    FirClassSymbol(currentClassId),
                    enumEntryName
                )
                firEnumEntry.annotations += modifier.annotations

                val delegatedSelfTypeRef = this.simpleIdentifier().nameAsSafeName().toDelegatedSelfType()
                val delegatedSuperTypeRef: FirTypeRef?
                var superTypeCallEntry: KotlinParser.ClassParametersContext? = null

                if (primaryConstructor != null && primaryConstructor.classParameters()?.classParameter() != null) {
                    delegatedSuperTypeRef = baseEnum
                    firEnumEntry.superTypeRefs += delegatedSuperTypeRef
                    superTypeCallEntry = primaryConstructor.classParameters()
                } else {
                    delegatedSuperTypeRef = implicitAnyType
                }

                val firPrimaryConstructor = null.toFirConstructor(
                    superTypeCallEntry,
                    delegatedSuperTypeRef,
                    delegatedSelfTypeRef
                )
                firEnumEntry.declarations += firPrimaryConstructor

                this.classBody()?.classMemberDeclarations()?.classMemberDeclaration()?.forEach { declaration ->
                    //TODO secondary constructor?
                    firEnumEntry.declarations += declaration.visitClassMemberDeclaration()//visitClassMemberDeclaration(declaration)
                }
                return firEnumEntry
            }
        }

        override fun visitEnumEntry(ctx: KotlinParser.EnumEntryContext): FirDeclaration {
            TODO("not used")
        }

        override fun visitFunctionDeclaration(ctx: KotlinParser.FunctionDeclarationContext): FirDeclaration {
            val receiverType = ctx.receiverType().convertSafe<FirTypeRef>()
            val functionName = Name.identifier(ctx.simpleIdentifier().text)
            val modifier = visitModifiers(ctx.modifiers())
            val returnType = if (ctx.functionBody()?.block() != null || modifier.inheritanceModifier == InheritanceModifier.ABSTRACT) {
                ctx.type().toFirOrUnitType()
            } else {
                ctx.type().toFirOrImplicitType()
            }

            val firFunction = FirMemberFunctionImpl(
                session,
                null,
                FirFunctionSymbol(callableIdForName(functionName)),
                functionName,
                modifier.visibilityModifier.toVisibility(),
                modifier.inheritanceModifier?.toModality(),
                modifier.platformModifier == PlatformModifier.EXPECT,
                modifier.platformModifier == PlatformModifier.ACTUAL,
                modifier.memberModifier == MemberModifier.OVERRIDE,
                modifier.functionModifier == FunctionModifier.OPERATOR,
                modifier.functionModifier == FunctionModifier.INFIX,
                modifier.functionModifier == FunctionModifier.INLINE,
                modifier.functionModifier == FunctionModifier.TAILREC,
                modifier.functionModifier == FunctionModifier.EXTERNAL,
                modifier.functionModifier == FunctionModifier.SUSPEND,
                receiverType,
                returnType
            )

            firFunctions += firFunction
            firFunction.annotations += modifier.annotations

            firFunction.typeParameters += ctx.typeParameters()?.typeParameter()?.map(this::visitTypeParameter) ?: listOf()
            firFunction.typeParameters.forEach { typeParameter ->
                ctx.typeConstraints()?.typeConstraint()?.forEach { typeConstraint ->
                    if (typeConstraint.simpleIdentifier().text == typeParameter.name.identifier) {
                        (typeParameter as FirTypeParameterImpl).bounds += visitTypeConstraint(typeConstraint)
                    }
                }
            }

            ctx.functionValueParameters()?.functionValueParameter()?.forEach {
                firFunction.valueParameters += visitFunctionValueParameter(it)
            }

            firFunction.body = visitFunctionBody(ctx.functionBody())
            firFunctions.removeLast()
            return firFunction
        }

        override fun visitFunctionValueParameters(ctx: KotlinParser.FunctionValueParametersContext?): FirElement {
            TODO("not used")
        }

        override fun visitFunctionValueParameter(ctx: KotlinParser.FunctionValueParameterContext): FirValueParameter {
            val modifier = visitModifiers(ctx.modifiers())
            val parameter = visitParameter(ctx.parameter())
            return FirValueParameterImpl(
                session,
                null,
                parameter.name,
                parameter.returnTypeRef,
                ctx.expression()?.let { visitExpression(it) },
                isCrossinline = modifier.parameterModifier == ParameterModifier.CROSSINLINE,
                isNoinline = modifier.parameterModifier == ParameterModifier.NOINLINE,
                isVararg = modifier.parameterModifier == ParameterModifier.VARARG
            ).apply { annotations += modifier.annotations }
        }

        override fun visitParameter(ctx: KotlinParser.ParameterContext): FirValueParameter {
            return FirValueParameterImpl(
                session,
                null,
                Name.identifier(ctx.simpleIdentifier().text),
                visitType(ctx.type()),
                null,
                isCrossinline = false,
                isNoinline = false,
                isVararg = false
            )
        }

        override fun visitSetterParameter(ctx: KotlinParser.SetterParameterContext?): FirElement {
            TODO("not used")
        }

        override fun visitFunctionBody(ctx: KotlinParser.FunctionBodyContext?): FirBlock? {
            return when {
                ctx == null -> null
                ctx.block() != null -> if (!stubMode) {
                    visitBlock(ctx.block())
                } else {
                    FirSingleExpressionBlock(
                        session,
                        FirExpressionStub(session, null).toReturn()
                    )
                }
                else -> {
                    //val result = { bodyExpression }.toFirExpression("Function has no body (but should)")
                    FirSingleExpressionBlock(
                        session,
                        visitExpression(ctx.expression()).toReturn()
                    )
                }
            }
        }

        override fun visitObjectDeclaration(ctx: KotlinParser.ObjectDeclarationContext): FirElement {
            val modifier = visitModifiers(ctx.modifiers())
            val classKind = ClassKind.OBJECT

            return visitClass(
                ctx.simpleIdentifier().nameAsSafeName(),
                classKind,
                modifier,
                null,
                null,
                null,
                ctx.delegationSpecifiers(),
                ctx.classBody(),
                null,
                false
            )
        }

        override fun visitCompanionObject(ctx: KotlinParser.CompanionObjectContext): FirElement {
            val modifier = visitModifiers(ctx.modifiers())
            val classKind = ClassKind.OBJECT

            return visitClass(
                ctx.simpleIdentifier().nameAsSafeName("Companion"),
                classKind,
                modifier,
                null,
                null,
                null,
                ctx.delegationSpecifiers(),
                ctx.classBody(),
                null,
                true
            )
        }

        private fun KotlinParser.PropertyDeclarationContext.visitProperty(): List<FirDeclaration> {
            if (this.variableDeclaration() != null) {
                return listOf(this.variableDeclaration().toFirProperty(this))
            } else {
                return this.multiVariableDeclaration().variableDeclaration().map { it.toFirProperty(this) }
            }
        }

        override fun visitPropertyDeclaration(ctx: KotlinParser.PropertyDeclarationContext): FirElement {
            if (ctx.variableDeclaration() != null) {
                return ctx.variableDeclaration().toFirProperty(ctx)
            } else {
                TODO("not implemented")
            }
        }

        override fun visitMultiVariableDeclaration(ctx: KotlinParser.MultiVariableDeclarationContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        private fun KotlinParser.VariableDeclarationContext.toFirProperty(
            propertyContext: KotlinParser.PropertyDeclarationContext
        ): FirDeclaration {
            val propertyType = this.type().toFirOrImplicitType()
            val propertyName = Name.identifier(this.simpleIdentifier().text)
            val modifier = visitModifiers(propertyContext.modifiers())
            val annotationContainer = this.annotation().map(this@Visitor::visitAnnotation).map { it.annotations }.flatten()
            val isVar = propertyContext.VAR() != null
            val initializer = if (propertyContext.expression() != null) {
                FirExpressionStub(session, null)
                //TODO("not implemented")
                //{ property.initializer }.toFirExpression("Should have initializer")
            } else null
            val firProperty =
                if (propertyContext.parent.parent is KotlinParser.StatementsContext) { //TODO how to understand that property is local
                    FirVariableImpl(
                        session,
                        null,
                        propertyName,
                        propertyType,
                        isVar,
                        initializer,
                        if (propertyContext.propertyDelegate() != null) {
                            FirExpressionStub(session, null)
                            //TODO("not implemented")
                            //{ property.delegate?.expression }.toFirExpression("Should have delegate")
                        } else null
                    )
                } else {
                    FirMemberPropertyImpl(
                        session,
                        null,
                        FirPropertySymbol(callableIdForName(propertyName)),
                        propertyName,
                        modifier.visibilityModifier.toVisibility(),
                        modifier.inheritanceModifier?.toModality(),
                        modifier.platformModifier == PlatformModifier.EXPECT,
                        modifier.platformModifier == PlatformModifier.ACTUAL,
                        modifier.memberModifier == MemberModifier.OVERRIDE,
                        modifier.propertyModifier == PropertyModifier.CONST,
                        modifier.memberModifier == MemberModifier.LATEINIT,
                        visitReceiverType(propertyContext.receiverType()),
                        propertyType,
                        isVar,
                        initializer,
                        propertyContext.getter().toFirPropertyAccessor(modifier.visibilityModifier.toVisibility(), propertyType),
                        propertyContext.setter().toFirPropertyAccessor(modifier.visibilityModifier.toVisibility(), propertyType),
                        if (propertyContext.propertyDelegate() != null) {
                            FirExpressionStub(session, null)
                            //TODO("not implemented")
                            //{ property.delegate?.expression }.toFirExpression("Should have delegate")
                        } else null
                    ).apply {
                        this.typeParameters += propertyContext.typeParameters()?.typeParameter()?.map(this@Visitor::visitTypeParameter)
                            ?: listOf()
                        this.typeParameters.forEach { typeParameter ->
                            propertyContext.typeConstraints()?.typeConstraint()?.forEach { typeConstraint ->
                                if (typeConstraint.simpleIdentifier().text == typeParameter.name.identifier) {
                                    (typeParameter as FirTypeParameterImpl).bounds += visitTypeConstraint(typeConstraint)
                                    typeParameter.annotations += typeConstraint.annotation().map(this@Visitor::visitAnnotation)
                                        .map { it.annotations }.flatten()
                                }
                            }
                        }
                    }
                }
            firProperty.annotations += modifier.annotations + annotationContainer
            return firProperty
        }

        override fun visitVariableDeclaration(ctx: KotlinParser.VariableDeclarationContext?): FirElement {
            TODO("not used")
        }

        override fun visitPropertyDelegate(ctx: KotlinParser.PropertyDelegateContext?): FirExpression {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        private fun KotlinParser.GetterContext?.toFirPropertyAccessor(
            propertyVisibility: Visibility,
            propertyTypeRef: FirTypeRef
        ): FirPropertyAccessor {
            if (this == null) {
                return FirDefaultPropertyGetter(session, null, propertyTypeRef, propertyVisibility)
            }
            val modifier = visitModifiers(this.modifiers())
            val firAccessor = FirPropertyAccessorImpl(
                session,
                null,
                true,
                modifier.visibilityModifier.toVisibility(),
                this.type()?.convertSafe() ?: propertyTypeRef
            )
            firFunctions += firAccessor
            firAccessor.annotations += modifier.annotations

            firAccessor.body = visitFunctionBody(this.functionBody())
            firFunctions.removeLast()
            return firAccessor
        }

        override fun visitGetter(ctx: KotlinParser.GetterContext?): FirElement {
            TODO("not used")
        }

        private fun KotlinParser.SetterContext?.toFirPropertyAccessor(
            propertyVisibility: Visibility,
            propertyTypeRef: FirTypeRef
        ): FirPropertyAccessor {
            if (this == null) {
                return FirDefaultPropertySetter(session, null, propertyTypeRef, propertyVisibility)
            }
            val modifier = visitModifiers(this.modifiers())
            val firAccessor = FirPropertyAccessorImpl(
                session,
                null,
                false,
                modifier.visibilityModifier.toVisibility(),
                this.type().toFirOrUnitType()
            )
            firFunctions += firAccessor
            firAccessor.annotations += modifier.annotations

            if (this.setterParameter() != null) {
                val setterParameterName = this.setterParameter().simpleIdentifier().text
                val setterParameterType =
                    if (this.setterParameter().type() != null) visitType(this.setterParameter().type()) else propertyTypeRef
                val parameterModifiers = this.parameterModifier()?.map { ParameterModifier.valueOf(it.text.toUpperCase()) }
                val firValueParameter = FirValueParameterImpl(
                    session,
                    null,
                    Name.identifier(setterParameterName),
                    setterParameterType,
                    null,
                    isCrossinline = parameterModifiers?.contains(ParameterModifier.CROSSINLINE) ?: false,
                    isNoinline = parameterModifiers?.contains(ParameterModifier.NOINLINE) ?: false,
                    isVararg = parameterModifiers?.contains(ParameterModifier.VARARG) ?: false
                ).apply {
                    annotations += this@toFirPropertyAccessor.annotation()?.map(this@Visitor::visitAnnotation)?.map { it.annotations }?.flatten()
                        ?: listOf()
                }
                firAccessor.valueParameters += firValueParameter
            }

            if (firAccessor.valueParameters.isEmpty()) {
                firAccessor.valueParameters += FirDefaultSetterValueParameter(session, null, propertyTypeRef)
            }
            firAccessor.body = visitFunctionBody(this.functionBody())
            firFunctions.removeLast()
            return firAccessor
        }

        override fun visitSetter(ctx: KotlinParser.SetterContext?): FirElement {
            TODO("not used")
        }

        override fun visitTypeAlias(ctx: KotlinParser.TypeAliasContext): FirElement {
            val modifier = visitModifiers(ctx.modifiers())
            val typeAliasName = Name.identifier(ctx.simpleIdentifier().text)
            return withChildClassName(typeAliasName) {
                val firTypeAlias = FirTypeAliasImpl(
                    session,
                    null,
                    FirTypeAliasSymbol(currentClassId),
                    typeAliasName,
                    modifier.visibilityModifier.toVisibility(),
                    modifier.platformModifier == PlatformModifier.EXPECT,
                    modifier.platformModifier == PlatformModifier.ACTUAL,
                    visitType(ctx.type())
                )
                firTypeAlias.annotations += modifier.annotations
                firTypeAlias.typeParameters += ctx.typeParameters()?.typeParameter()?.map(this::visitTypeParameter) ?: listOf()

                return firTypeAlias
            }
        }

        override fun visitTypeParameters(ctx: KotlinParser.TypeParametersContext?): FirElement {
            TODO("not used")
        }

        override fun visitTypeParameter(ctx: KotlinParser.TypeParameterContext): FirTypeParameter {
            val parameterName = ctx.simpleIdentifier().text
            val parameterModifiers = visitTypeParameterModifiers(ctx.typeParameterModifiers())
            val firTypeParameter = FirTypeParameterImpl(
                session,
                null,
                FirTypeParameterSymbol(),
                Name.identifier(parameterName),
                parameterModifiers.varianceModifier.toVariance(),
                parameterModifiers.reificationModifier != null
            )
            firTypeParameter.annotations += parameterModifiers.annotations
            if (ctx.type() != null) {
                firTypeParameter.bounds += ctx.type().convert<FirTypeRef>()
            }
            return firTypeParameter
        }

        override fun visitTypeParameterModifiers(ctx: KotlinParser.TypeParameterModifiersContext?): TypeParameterModifier {
            return ctx?.let {
                return it.typeParameterModifier().map(this::visitTypeParameterModifier).fold(
                    TypeParameterModifier(session),
                    operation = { m1, m2 ->
                        return TypeParameterModifier(
                            session,
                            null,
                            if (m1.varianceModifier.toVariance() != Variance.INVARIANT) m1.varianceModifier
                            else m2.varianceModifier,
                            m1.reificationModifier ?: m2.reificationModifier
                        ).apply { annotations += m1.annotations + m2.annotations }
                    }
                )
            } ?: TypeParameterModifier(session)
        }

        override fun visitTypeParameterModifier(ctx: KotlinParser.TypeParameterModifierContext): TypeParameterModifier {
            val annotationContainer = visitAnnotation(ctx.annotation())
            return TypeParameterModifier(
                session,
                null,
                ctx.varianceModifier()?.let {
                    VarianceModifier.valueOf(it.text.toUpperCase())
                } ?: VarianceModifier.INVARIANT,
                ctx.reificationModifier()?.let { ReificationModifier.valueOf(it.text.toUpperCase()) }
            ).apply { annotations += annotationContainer.annotations }
        }

        private fun isNullable(currentNode: RuleContext): Boolean {
            if (currentNode is KotlinParser.NullableTypeContext) {
                return true
            }
            if (currentNode is KotlinParser.TypeContext ||
                currentNode is KotlinParser.ParenthesizedTypeContext ||
                currentNode is KotlinParser.TypeReferenceContext ||
                currentNode is KotlinParser.FunctionTypeContext ||
                currentNode is KotlinParser.ReceiverTypeContext
            ) {
                return isNullable(currentNode.parent)
            }
            return false
        }

        override fun visitType(ctx: KotlinParser.TypeContext?): FirTypeRef {
            val firTypeRef = when {
                ctx == null -> return FirErrorTypeRefImpl(session, null, "Type is null")
                ctx.parenthesizedType() != null -> visitParenthesizedType(ctx.parenthesizedType())
                ctx.nullableType() != null -> visitNullableType(ctx.nullableType())
                ctx.typeReference() != null -> visitTypeReference(ctx.typeReference())
                ctx.functionType() != null -> visitFunctionType(ctx.functionType())
                else -> throw AssertionError("Unexpected type element")
            } as FirAbstractAnnotatedTypeRef

            val modifier = visitTypeModifiers(ctx.typeModifiers())
            firTypeRef.annotations += modifier.annotations

            return firTypeRef
        }

        override fun visitTypeModifiers(ctx: KotlinParser.TypeModifiersContext?): TypeModifier {
            return ctx?.let {
                return it.typeModifier().map(this::visitTypeModifier).fold(
                    TypeModifier(session),
                    operation = { m1, m2 ->
                        return TypeModifier(
                            session,
                            null,
                            m1.suspendModifier ?: m2.suspendModifier
                        ).apply { annotations += m1.annotations + m2.annotations }
                    }
                )
            } ?: TypeModifier(session)
        }

        override fun visitTypeModifier(ctx: KotlinParser.TypeModifierContext): TypeModifier {
            val annotationContainer = visitAnnotation(ctx.annotation())
            return TypeModifier(
                session,
                null,
                ctx.SUSPEND()?.let { SuspendModifier.valueOf(it.text.toUpperCase()) }
            ).apply { annotations += annotationContainer.annotations }
        }

        override fun visitParenthesizedType(ctx: KotlinParser.ParenthesizedTypeContext): FirTypeRef {
            return visitType(ctx.type())
        }

        override fun visitNullableType(ctx: KotlinParser.NullableTypeContext): FirTypeRef {
            return when {
                ctx.parenthesizedType() != null -> visitParenthesizedType(ctx.parenthesizedType())
                ctx.typeReference() != null -> visitTypeReference(ctx.typeReference())
                else -> throw AssertionError("Unexpected nullable type element")
            }
        }

        override fun visitTypeReference(ctx: KotlinParser.TypeReferenceContext): FirTypeRef {
            if (ctx.DYNAMIC() != null) {
                return FirDynamicTypeRefImpl(
                    session,
                    null,
                    isNullable(ctx.parent)
                )
            }
            return visitUserType(ctx.userType())
        }

        override fun visitFunctionType(ctx: KotlinParser.FunctionTypeContext): FirTypeRef {
            val receiverTypeReference = visitReceiverType(ctx.receiverType())
            val returnTypeReference = visitType(ctx.type())
            val functionType = FirFunctionTypeRefImpl(
                session,
                null,
                isNullable(ctx.parent),
                receiverTypeReference,
                returnTypeReference
            )
            for (valueParameter in ctx.functionTypeParameters().children) {
                when (valueParameter) {
                    is KotlinParser.ParameterContext -> functionType.valueParameters += visitParameter(valueParameter)
                    is KotlinParser.TypeContext -> {
                        val firType = visitType(valueParameter)
                        functionType.valueParameters += FirValueParameterImpl(
                            session,
                            null,
                            SpecialNames.NO_NAME_PROVIDED,
                            firType,
                            null,
                            isCrossinline = false,
                            isNoinline = false,
                            isVararg = false
                        )
                    }
                }
            }
            return functionType
        }

        override fun visitReceiverType(ctx: KotlinParser.ReceiverTypeContext?): FirTypeRef? {
            val firTypeRef = when {
                ctx == null -> return null
                ctx.parenthesizedType() != null -> ctx.parenthesizedType().convert<FirAbstractAnnotatedTypeRef>()
                ctx.nullableType() != null -> ctx.nullableType().convert()
                ctx.typeReference() != null -> ctx.typeReference().convert()
                else -> throw AssertionError("Unexpected receiver type element")
            }

            val modifier = visitTypeModifiers(ctx.typeModifiers())
            firTypeRef.annotations += modifier.annotations

            return firTypeRef
        }

        override fun visitUserType(ctx: KotlinParser.UserTypeContext): FirUserTypeRef {
            val firUserTypeRef = FirUserTypeRefImpl(
                session,
                null,
                isNullable(ctx.parent)
            )
            for (simpleUserType in ctx.simpleUserType()) {
                val simpleFirUserType = visitSimpleUserType(simpleUserType)
                firUserTypeRef.qualifier.addAll(simpleFirUserType.qualifier)
            }
            //firUserTypeRef.qualifier.reverse()

            return firUserTypeRef
        }

        override fun visitParenthesizedUserType(ctx: KotlinParser.ParenthesizedUserTypeContext?): FirElement {
            TODO("not used")
        }

        override fun visitSimpleUserType(ctx: KotlinParser.SimpleUserTypeContext): FirUserTypeRef {
            val qualifier = FirQualifierPartImpl(
                Name.identifier(ctx.simpleIdentifier().text)
            )
            if (ctx.typeArguments() != null) {
                for (typeProjection in ctx.typeArguments().typeProjection()) {
                    qualifier.typeArguments += visitTypeProjection(typeProjection)
                }
            }

            //firUserTypeRef.qualifier.reverse()
            return FirUserTypeRefImpl(
                session,
                null,
                false
            ).apply { this.qualifier.add(qualifier) }
        }

        override fun visitFunctionTypeParameters(ctx: KotlinParser.FunctionTypeParametersContext?): FirElement {
            TODO("not used")
        }

        override fun visitTypeConstraints(ctx: KotlinParser.TypeConstraintsContext?): FirElement {
            TODO("not used")
        }

        override fun visitTypeConstraint(ctx: KotlinParser.TypeConstraintContext): FirTypeRef {
            return (ctx.type().convert<FirUserTypeRefImpl>())
                .apply {
                    annotations += ctx.annotation().map(this@Visitor::visitAnnotation).map { it.annotations }.flatten()
                }
        }

        override fun visitBlock(ctx: KotlinParser.BlockContext?): FirBlock? {
            return when (ctx) {
                null -> null
                else -> if (!stubMode) {
                    visitStatements(ctx.statements())
                } else {
                    FirSingleExpressionBlock(
                        session,
                        FirExpressionStub(session, null).toReturn()
                    )
                }
            }
        }

        override fun visitStatements(ctx: KotlinParser.StatementsContext?): FirBlock {
            return FirBlockImpl(session, null).apply {
                ctx?.statement()?.forEach { statement ->
                    val firStatement = visitStatement(statement)
                    if (firStatement !is FirBlock || firStatement.annotations.isNotEmpty()) {
                        statements += firStatement
                    } else {
                        statements += firStatement.statements
                    }
                }
            }
        }

        override fun visitStatement(ctx: KotlinParser.StatementContext?): FirStatement {
            TODO("not implemented")
        }

        private fun KotlinParser.DeclarationContext.visitDeclaration(): List<FirDeclaration> {
            return when {
                this.classDeclaration() != null -> listOf(this.classDeclaration().convert())
                this.objectDeclaration() != null -> listOf(this.objectDeclaration().convert())
                this.functionDeclaration() != null -> listOf(this.functionDeclaration().convert())
                this.propertyDeclaration() != null -> this.propertyDeclaration().visitProperty()
                this.typeAlias() != null -> listOf(this.typeAlias().convert())
                else -> throw AssertionError("Unexpected declaration")
            }
        }

        override fun visitDeclaration(ctx: KotlinParser.DeclarationContext): FirDeclaration {
            return when {
                ctx.classDeclaration() != null -> ctx.classDeclaration().convert()
                ctx.objectDeclaration() != null -> ctx.objectDeclaration().convert()
                ctx.functionDeclaration() != null -> ctx.functionDeclaration().convert()
                ctx.propertyDeclaration() != null -> ctx.propertyDeclaration().convert()
                ctx.typeAlias() != null -> ctx.typeAlias().convert()
                else -> throw AssertionError("Unexpected declaration")
            }
        }

        override fun visitAssignment(ctx: KotlinParser.AssignmentContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitExpression(ctx: KotlinParser.ExpressionContext?): FirExpression {
            return if (stubMode) FirExpressionStub(session, null)
            else TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitDisjunction(ctx: KotlinParser.DisjunctionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitConjunction(ctx: KotlinParser.ConjunctionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitEquality(ctx: KotlinParser.EqualityContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitComparison(ctx: KotlinParser.ComparisonContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitInfixOperation(ctx: KotlinParser.InfixOperationContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitElvisExpression(ctx: KotlinParser.ElvisExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitInfixFunctionCall(ctx: KotlinParser.InfixFunctionCallContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitRangeExpression(ctx: KotlinParser.RangeExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAdditiveExpression(ctx: KotlinParser.AdditiveExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitMultiplicativeExpression(ctx: KotlinParser.MultiplicativeExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAsExpression(ctx: KotlinParser.AsExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitPrefixUnaryExpression(ctx: KotlinParser.PrefixUnaryExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitUnaryPrefix(ctx: KotlinParser.UnaryPrefixContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitPostfixUnaryExpression(ctx: KotlinParser.PostfixUnaryExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitPostfixUnarySuffix(ctx: KotlinParser.PostfixUnarySuffixContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitDirectlyAssignableExpression(ctx: KotlinParser.DirectlyAssignableExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAssignableExpression(ctx: KotlinParser.AssignableExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAssignableSuffix(ctx: KotlinParser.AssignableSuffixContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitIndexingSuffix(ctx: KotlinParser.IndexingSuffixContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitNavigationSuffix(ctx: KotlinParser.NavigationSuffixContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitCallSuffix(ctx: KotlinParser.CallSuffixContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAnnotatedLambda(ctx: KotlinParser.AnnotatedLambdaContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitValueArguments(ctx: KotlinParser.ValueArgumentsContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitTypeArguments(ctx: KotlinParser.TypeArgumentsContext?): FirElement {
            TODO("not used")
        }

        override fun visitTypeProjection(ctx: KotlinParser.TypeProjectionContext): FirTypeProjection {
            val typePropertyModifier = visitTypeProjectionModifiers(ctx.typeProjectionModifiers())
            //TODO what with annotations?
            return if (ctx.MULT() != null) FirStarProjectionImpl(session, null)
            else FirTypeProjectionWithVarianceImpl(
                session,
                null,
                typePropertyModifier.varianceModifier.toVariance(),
                visitType(ctx.type())
            )
        }

        override fun visitTypeProjectionModifiers(ctx: KotlinParser.TypeProjectionModifiersContext?): TypeProjectionModifier {
            return ctx?.let {
                return it.typeProjectionModifier().map(this::visitTypeProjectionModifier).fold(
                    TypeProjectionModifier(session),
                    operation = { m1, m2 ->
                        return TypeProjectionModifier(
                            session,
                            null,
                            if (m1.varianceModifier.toVariance() != Variance.INVARIANT) m1.varianceModifier
                            else m2.varianceModifier
                        ).apply { annotations += m1.annotations + m2.annotations }
                    }
                )
            } ?: TypeProjectionModifier(session)
        }

        override fun visitTypeProjectionModifier(ctx: KotlinParser.TypeProjectionModifierContext): TypeProjectionModifier {
            val annotationContainer = visitAnnotation(ctx.annotation())
            return TypeProjectionModifier(
                session,
                null,
                ctx.varianceModifier()?.let {
                    VarianceModifier.valueOf(it.text.toUpperCase())
                } ?: VarianceModifier.INVARIANT
            ).apply { annotations += annotationContainer.annotations }
        }

        override fun visitValueArgument(ctx: KotlinParser.ValueArgumentContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitPrimaryExpression(ctx: KotlinParser.PrimaryExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitParenthesizedExpression(ctx: KotlinParser.ParenthesizedExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitCollectionLiteral(ctx: KotlinParser.CollectionLiteralContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitLiteralConstant(ctx: KotlinParser.LiteralConstantContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitStringLiteral(ctx: KotlinParser.StringLiteralContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitLineStringLiteral(ctx: KotlinParser.LineStringLiteralContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitMultiLineStringLiteral(ctx: KotlinParser.MultiLineStringLiteralContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitLineStringContent(ctx: KotlinParser.LineStringContentContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitLineStringExpression(ctx: KotlinParser.LineStringExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitMultiLineStringContent(ctx: KotlinParser.MultiLineStringContentContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitMultiLineStringExpression(ctx: KotlinParser.MultiLineStringExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitLambdaLiteral(ctx: KotlinParser.LambdaLiteralContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitLambdaParameters(ctx: KotlinParser.LambdaParametersContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitLambdaParameter(ctx: KotlinParser.LambdaParameterContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAnonymousFunction(ctx: KotlinParser.AnonymousFunctionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitFunctionLiteral(ctx: KotlinParser.FunctionLiteralContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitObjectLiteral(ctx: KotlinParser.ObjectLiteralContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitThisExpression(ctx: KotlinParser.ThisExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitSuperExpression(ctx: KotlinParser.SuperExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitControlStructureBody(ctx: KotlinParser.ControlStructureBodyContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitIfExpression(ctx: KotlinParser.IfExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitWhenExpression(ctx: KotlinParser.WhenExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitWhenEntry(ctx: KotlinParser.WhenEntryContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitWhenCondition(ctx: KotlinParser.WhenConditionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitRangeTest(ctx: KotlinParser.RangeTestContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitTypeTest(ctx: KotlinParser.TypeTestContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitTryExpression(ctx: KotlinParser.TryExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitCatchBlock(ctx: KotlinParser.CatchBlockContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitFinallyBlock(ctx: KotlinParser.FinallyBlockContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitLoopStatement(ctx: KotlinParser.LoopStatementContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitForStatement(ctx: KotlinParser.ForStatementContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitWhileStatement(ctx: KotlinParser.WhileStatementContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitDoWhileStatement(ctx: KotlinParser.DoWhileStatementContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitJumpExpression(ctx: KotlinParser.JumpExpressionContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitCallableReference(ctx: KotlinParser.CallableReferenceContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAssignmentAndOperator(ctx: KotlinParser.AssignmentAndOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitEqualityOperator(ctx: KotlinParser.EqualityOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitComparisonOperator(ctx: KotlinParser.ComparisonOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitInOperator(ctx: KotlinParser.InOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitIsOperator(ctx: KotlinParser.IsOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAdditiveOperator(ctx: KotlinParser.AdditiveOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitMultiplicativeOperator(ctx: KotlinParser.MultiplicativeOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAsOperator(ctx: KotlinParser.AsOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitPrefixUnaryOperator(ctx: KotlinParser.PrefixUnaryOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitPostfixUnaryOperator(ctx: KotlinParser.PostfixUnaryOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitMemberAccessOperator(ctx: KotlinParser.MemberAccessOperatorContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitModifiers(ctx: KotlinParser.ModifiersContext?): Modifier {
            return ctx?.let {
                val annotationContainer = it.annotation().map(this::visitAnnotation).fold(
                    AnnotationContainer(session, mutableListOf()),
                    operation = { a1, a2 ->
                        AnnotationContainer(session, a1.annotations + a2.annotations)
                    }
                )
                return it.modifier().map(this::visitModifier).fold(
                    Modifier(session),
                    operation = { m1, m2 ->
                        Modifier(
                            session,
                            null,
                            m1.classModifier ?: m2.classModifier,
                            m1.memberModifier ?: m2.memberModifier,
                            if (m1.visibilityModifier.toVisibility() != Visibilities.UNKNOWN) m1.visibilityModifier
                            else m2.visibilityModifier,
                            m1.functionModifier ?: m2.functionModifier,
                            m1.propertyModifier ?: m2.propertyModifier,
                            m1.inheritanceModifier ?: m2.inheritanceModifier,
                            m1.parameterModifier ?: m2.parameterModifier,
                            m1.platformModifier ?: m2.platformModifier
                        )
                    }
                ).apply { annotations += annotationContainer.annotations }
            } ?: Modifier(session)
        }

        override fun visitModifier(ctx: KotlinParser.ModifierContext): Modifier {
            return Modifier(
                session,
                null,
                ctx.classModifier()?.let { ClassModifier.valueOf(it.text.toUpperCase()) },
                ctx.memberModifier()?.let { MemberModifier.valueOf(it.text.toUpperCase()) },
                ctx.visibilityModifier()?.let {
                    VisibilityModifier.valueOf(it.text.toUpperCase())
                } ?: VisibilityModifier.UNKNOWN,
                ctx.functionModifier()?.let { FunctionModifier.valueOf(it.text.toUpperCase()) },
                ctx.propertyModifier()?.let { PropertyModifier.valueOf(it.text.toUpperCase()) },
                ctx.inheritanceModifier()?.let { InheritanceModifier.valueOf(it.text.toUpperCase()) },
                ctx.parameterModifier()?.let { ParameterModifier.valueOf(it.text.toUpperCase()) },
                ctx.platformModifier()?.let { PlatformModifier.valueOf(it.text.toUpperCase()) }
            )
        }

        override fun visitClassModifier(ctx: KotlinParser.ClassModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitMemberModifier(ctx: KotlinParser.MemberModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitVisibilityModifier(ctx: KotlinParser.VisibilityModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitVarianceModifier(ctx: KotlinParser.VarianceModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitFunctionModifier(ctx: KotlinParser.FunctionModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitPropertyModifier(ctx: KotlinParser.PropertyModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitInheritanceModifier(ctx: KotlinParser.InheritanceModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitParameterModifier(ctx: KotlinParser.ParameterModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitReificationModifier(ctx: KotlinParser.ReificationModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitPlatformModifier(ctx: KotlinParser.PlatformModifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitLabel(ctx: KotlinParser.LabelContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitAnnotation(ctx: KotlinParser.AnnotationContext?): AnnotationContainer {
            return when {
                ctx == null -> AnnotationContainer(session, mutableListOf())
                ctx.singleAnnotation() != null -> visitSingleAnnotation(ctx.singleAnnotation())
                ctx.multiAnnotation() != null -> visitMultiAnnotation(ctx.multiAnnotation())
                else -> AnnotationContainer(session, mutableListOf())
            }
        }

        override fun visitSingleAnnotation(ctx: KotlinParser.SingleAnnotationContext): AnnotationContainer {
            return AnnotationContainer(
                session,
                listOf(
                    FirAnnotationCallImpl(
                        session,
                        null,
                        ctx.annotationUseSiteTarget().annotationUseSiteTarget,
                        visitUnescapedAnnotation(ctx.unescapedAnnotation()).annotationTypeRef
                    )
                )
            )
        }

        override fun visitMultiAnnotation(ctx: KotlinParser.MultiAnnotationContext): AnnotationContainer {
            val annotationUseSiteTarget = ctx.annotationUseSiteTarget().annotationUseSiteTarget
            val annotationContainer = AnnotationContainer(session, null)
            ctx.unescapedAnnotation().forEach {
                annotationContainer.annotations += FirAnnotationCallImpl(
                    session,
                    null,
                    annotationUseSiteTarget,
                    visitUnescapedAnnotation(it).annotationTypeRef
                )
            }
            return annotationContainer
        }

        private val KotlinParser.AnnotationUseSiteTargetContext?.annotationUseSiteTarget: AnnotationUseSiteTarget?
            get() = when (this) {
                null -> null
                this.AT_FIELD() -> AnnotationUseSiteTarget.FIELD
                this.AT_PROPERTY() -> AnnotationUseSiteTarget.PROPERTY
                this.AT_GET() -> AnnotationUseSiteTarget.PROPERTY_GETTER
                this.AT_SET() -> AnnotationUseSiteTarget.PROPERTY_SETTER
                this.AT_RECEIVER() -> AnnotationUseSiteTarget.RECEIVER
                this.AT_PARAM() -> AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
                this.AT_SETPARAM() -> AnnotationUseSiteTarget.SETTER_PARAMETER
                this.AT_DELEGATE() -> AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
                else -> null
            }

        override fun visitAnnotationUseSiteTarget(ctx: KotlinParser.AnnotationUseSiteTargetContext?): FirElement {
            TODO("not used")
        }

        override fun visitUnescapedAnnotation(ctx: KotlinParser.UnescapedAnnotationContext?): FirAnnotationCall {
            //TODO("not implemented")
            return FirAnnotationCallImpl(
                session,
                null,
                null,
                FirErrorTypeRefImpl(session, null, "not implemented")
            )
        }

        private fun KotlinParser.SimpleIdentifierContext?.nameAsSafeName(defaultName: String = ""): Name {
            return when {
                this != null -> Name.identifier(this.text)
                defaultName.isNotEmpty() -> Name.identifier(defaultName)
                else -> SpecialNames.NO_NAME_PROVIDED
            }
        }

        private fun Name?.toDelegatedSelfType(): FirTypeRef =
            FirUserTypeRefImpl(session, null, isNullable = false).apply {
                qualifier.add(
                    FirQualifierPartImpl(
                        this@toDelegatedSelfType ?: SpecialNames.NO_NAME_PROVIDED
                    )
                )
            }

        override fun visitSimpleIdentifier(ctx: KotlinParser.SimpleIdentifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitIdentifier(ctx: KotlinParser.IdentifierContext?): FirElement {
            TODO("not used")
        }

        override fun visitShebangLine(ctx: KotlinParser.ShebangLineContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitQuest(ctx: KotlinParser.QuestContext?): FirElement {
            TODO("not used")
        }

        override fun visitElvis(ctx: KotlinParser.ElvisContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitSafeNav(ctx: KotlinParser.SafeNavContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitExcl(ctx: KotlinParser.ExclContext?): FirElement {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitSemi(ctx: KotlinParser.SemiContext?): FirElement {
            TODO("not used")
        }

        override fun visitSemis(ctx: KotlinParser.SemisContext?): FirElement {
            TODO("not used")
        }
    }
}