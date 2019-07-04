/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.lightTree.fir.TypeConstraint
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

object ConverterUtil {
    fun String?.nameAsSafeName(defaultName: String = ""): Name {
        return when {
            this != null -> Name.identifier(this.replace("`", ""))
            defaultName.isNotEmpty() -> Name.identifier(defaultName)
            else -> SpecialNames.NO_NAME_PROVIDED
        }
    }

    fun String.toFirUserType(session: FirSession): FirUserTypeRef {
        val qualifier = FirQualifierPartImpl(
            Name.identifier(this)
        )

        return FirUserTypeRefImpl(
            session,
            null,
            false
        ).apply {
            this.qualifier.add(qualifier)
        }
    }

    fun toDelegatedSelfType(firClass: FirRegularClass): FirTypeRef {
        val typeParameters = firClass.typeParameters.map {
            FirTypeParameterImpl(firClass.session, it.psi, FirTypeParameterSymbol(), it.name, Variance.INVARIANT, false).apply {
                this.bounds += it.bounds
            }
        }
        return FirResolvedTypeRefImpl(
            firClass.session,
            null,
            ConeClassTypeImpl(
                firClass.symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        )
    }

    fun LighterASTNode?.hasSecondaryConstructor(kidsArray: Array<LighterASTNode?>): Boolean {
        if (this == null) return false
        //TODO check if node isn't CLASS_BODY

        for (kid in kidsArray) {
            if (kid == null) continue
            when (kid.tokenType) {
                KtNodeTypes.SECONDARY_CONSTRUCTOR -> return true
            }
        }
        return false
    }

    fun FirExpression.toReturn(labelName: String? = null): FirReturnExpression {
        return FirReturnExpressionImpl(
            session,
            null,
            this
        ).apply {
            target = FirFunctionTarget(labelName)
            val lastFunction = FunctionUtil.firFunctions.lastOrNull()
            if (labelName == null) {
                if (lastFunction != null) {
                    target.bind(lastFunction)
                } else {
                    target.bind(FirErrorFunction(session, psi, "Cannot bind unlabeled return to a function"))
                }
            } else {
                for (firFunction in FunctionUtil.firFunctions.asReversed()) {
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

    fun FirTypeParameterContainer.joinTypeParameters(typeConstraints: List<TypeConstraint>) {
        typeConstraints.forEach { (identifier, type) ->
            this.typeParameters.forEach { typeParameter ->
                if (identifier == typeParameter.name.identifier) {
                    (typeParameter as FirTypeParameterImpl).bounds += type
                    typeParameter.annotations += type.annotations
                }
            }
        }
    }

    fun typeParametersFromSelfType(delegatedSelfTypeRef: FirTypeRef): List<FirTypeParameter> {
        return delegatedSelfTypeRef.coneTypeSafe<ConeKotlinType>()
            ?.typeArguments
            ?.map { ((it as ConeTypeParameterType).lookupTag.symbol as FirTypeParameterSymbol).fir }
            ?: emptyList()
    }

    fun <T : FirCallWithArgumentList> T.extractArgumentsFrom(container: List<FirExpression>, stubMode: Boolean): T {
        if (!stubMode) {
            //TODO("not implemented")
            this.arguments += container
        }
        return this
    }

    fun List<FirEnumEntryImpl>.fillEnumEntryConstructor(
        firPrimaryConstructor: FirConstructor,
        enumType: FirUserTypeRef,
        stubMode: Boolean
    ) {
        this.forEach { firEnumEntry ->
            val enumDelegatedSelfTypeRef = toDelegatedSelfType(firEnumEntry)
            var enumDelegatedSuperTypeRef: FirTypeRef = FirImplicitAnyTypeRef(firEnumEntry.session, null)
            val enumSuperTypeCallEntry = mutableListOf<FirExpression>()

            if (firPrimaryConstructor.valueParameters.isNotEmpty()) {
                enumDelegatedSuperTypeRef = enumType//enumDelegatedSelfTypeRef
                enumSuperTypeCallEntry += firPrimaryConstructor.valueParameters.map { firValueParameter ->
                    firValueParameter.toFirExpression(stubMode)
                }
            }
            firEnumEntry.superTypeRefs += enumDelegatedSuperTypeRef

            val firDelegatedConstructorCall = FirDelegatedConstructorCallImpl(
                firEnumEntry.session,
                null,
                enumDelegatedSuperTypeRef,
                false
            ).extractArgumentsFrom(enumSuperTypeCallEntry, stubMode)

            val firEnumPrimaryConstructor = FirPrimaryConstructorImpl(
                firEnumEntry.session,
                null,
                FirFunctionSymbol(ClassNameUtil.callableIdForClassConstructor()),
                Visibilities.UNKNOWN,
                false,
                false,
                enumDelegatedSelfTypeRef,
                firDelegatedConstructorCall
            )
            firEnumPrimaryConstructor.typeParameters += typeParametersFromSelfType(enumDelegatedSelfTypeRef)
            firEnumEntry.declarations.add(0, firEnumPrimaryConstructor)
        }
    }

    fun List<FirDeclaration>.fillConstructors(
        hasPrimaryConstructor: Boolean,
        delegatedSelfTypeRef: FirTypeRef,
        delegatedSuperTypeRef: FirTypeRef
    ) {
        this.forEach { constructor ->
            (constructor as FirConstructorImpl).typeParameters += typeParametersFromSelfType(delegatedSelfTypeRef)

            constructor.returnTypeRef = delegatedSelfTypeRef
            val constructorDelegationCall = constructor.delegatedConstructor as? FirDelegatedConstructorCallImpl
            val isThis =
                (constructorDelegationCall == null && hasPrimaryConstructor) || constructorDelegationCall?.isThis == true
            val delegatedType = when {
                isThis -> delegatedSelfTypeRef
                else -> delegatedSuperTypeRef
            }
            constructorDelegationCall?.constructedTypeRef = delegatedType
        }
    }

    fun FirValueParameter.toFirExpression(stubMode: Boolean): FirExpression {
        return if (stubMode) FirExpressionStub(this.session, null)
        else TODO("not implemeted")
    }
}

object ClassNameUtil {
    lateinit var packageFqName: FqName

    inline fun <T> withChildClassName(name: Name, l: () -> T): T {
        className = className.child(name)
        val t = l()
        className = className.parent()
        return t
    }

    val currentClassId
        get() = ClassId(packageFqName, className, false)

    fun callableIdForName(name: Name, local: Boolean = false) =
        when {
            local -> CallableId(name)
            className == FqName.ROOT -> CallableId(packageFqName, name)
            else -> CallableId(packageFqName, className, name)
        }

    fun callableIdForClassConstructor() =
        if (className == FqName.ROOT) CallableId(packageFqName, Name.special("<anonymous-init>"))
        else CallableId(packageFqName, className, className.shortName())

    var className: FqName = FqName.ROOT
}

object FunctionUtil {
    val firFunctions = mutableListOf<FirFunction>()

    fun <T> MutableList<T>.removeLast() {
        removeAt(size - 1)
    }

    fun <T> MutableList<T>.pop(): T? {
        val result = lastOrNull()
        if (result != null) {
            removeAt(size - 1)
        }
        return result
    }
}

object DataClassUtil {
    fun generateComponentFunctions(
        session: FirSession, firClass: FirClassImpl, properties: List<FirProperty>
    ) {
        var componentIndex = 1
        for (property in properties) {
            if (!property.isVal && !property.isVar) continue
            val name = Name.identifier("component$componentIndex")
            componentIndex++
            val symbol = FirFunctionSymbol(
                CallableId(
                    ClassNameUtil.packageFqName,
                    ClassNameUtil.className,
                    name
                )
            )
            firClass.addDeclaration(
                FirMemberFunctionImpl(
                    session, null, symbol, name,
                    Visibilities.PUBLIC, Modality.FINAL,
                    isExpect = false, isActual = false,
                    isOverride = false, isOperator = false,
                    isInfix = false, isInline = false,
                    isTailRec = false, isExternal = false,
                    isSuspend = false, receiverTypeRef = null,
                    returnTypeRef = FirImplicitTypeRefImpl(session, null)
                ).apply {
                    val componentFunction = this
                    body = FirSingleExpressionBlock(
                        session,
                        FirReturnExpressionImpl(
                            session, null,
                            FirQualifiedAccessExpressionImpl(session, null).apply {
                                val parameterName = property.name
                                calleeReference = FirResolvedCallableReferenceImpl(
                                    session, null,
                                    parameterName, property.symbol
                                )
                            }
                        ).apply {
                            target = FirFunctionTarget(null)
                            target.bind(componentFunction)
                        }
                    )
                }
            )
        }
    }

    private val copyName = Name.identifier("copy")

    fun generateCopyFunction(
        session: FirSession, firClass: FirClassImpl,
        firPrimaryConstructor: FirConstructor,
        properties: List<FirProperty>
    ) {
        val symbol = FirFunctionSymbol(CallableId(ClassNameUtil.packageFqName, ClassNameUtil.className, copyName))
        firClass.addDeclaration(
            FirMemberFunctionImpl(
                session, null, symbol, copyName,
                Visibilities.PUBLIC, Modality.FINAL,
                isExpect = false, isActual = false,
                isOverride = false, isOperator = false,
                isInfix = false, isInline = false,
                isTailRec = false, isExternal = false,
                isSuspend = false, receiverTypeRef = null,
                returnTypeRef = firPrimaryConstructor.returnTypeRef//FirImplicitTypeRefImpl(session, this)
            ).apply {
                val copyFunction = this
                for (property in properties) {
                    val name = property.name
                    valueParameters += FirValueParameterImpl(
                        session, null, name,
                        property.returnTypeRef,
                        FirQualifiedAccessExpressionImpl(session, null).apply {
                            calleeReference = FirResolvedCallableReferenceImpl(session, null, name, property.symbol)
                        },
                        isCrossinline = false, isNoinline = false, isVararg = false
                    )
                }

                body = FirEmptyExpressionBlock(session)
            }
        )
    }
}

