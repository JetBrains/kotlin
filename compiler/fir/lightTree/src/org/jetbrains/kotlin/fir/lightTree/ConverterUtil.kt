/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractCall
import org.jetbrains.kotlin.fir.expressions.impl.FirDelegatedConstructorCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirReturnExpressionImpl
import org.jetbrains.kotlin.fir.lightTree.fir.TypeConstraint
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

object ConverterUtil {
    fun String?.nameAsSafeName(defaultName: String = ""): Name {
        return when {
            this != null -> Name.identifier(this)
            defaultName.isNotEmpty() -> Name.identifier(defaultName)
            else -> SpecialNames.NO_NAME_PROVIDED
        }
    }

    fun Name?.toDelegatedSelfType(session: FirSession): FirTypeRef =
        FirUserTypeRefImpl(session, null, isNullable = false).apply {
            qualifier.add(
                FirQualifierPartImpl(
                    this@toDelegatedSelfType ?: SpecialNames.NO_NAME_PROVIDED
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

    fun <T : FirAbstractCall> T.extractArgumentsFrom(container: List<FirExpression>, stubMode: Boolean): T {
        if (!stubMode) {
            //TODO("not implemented")
            this.arguments += container
        }
        return this
    }

    fun List<FirEnumEntryImpl>.fillEnumEntryConstructor(
        firPrimaryConstructor: FirConstructor,
        stubMode: Boolean
    ) {
        this.forEach { firEnumEntry ->
            val enumDelegatedSelfTypeRef = firPrimaryConstructor.returnTypeRef
            var enumDelegatedSuperTypeRef: FirTypeRef = FirImplicitAnyTypeRef(firEnumEntry.session, null)
            val enumSuperTypeCallEntry = mutableListOf<FirExpression>()

            if (firPrimaryConstructor.valueParameters.isNotEmpty()) {
                enumDelegatedSuperTypeRef = enumDelegatedSelfTypeRef
                firEnumEntry.superTypeRefs += enumDelegatedSuperTypeRef
                enumSuperTypeCallEntry += firPrimaryConstructor.valueParameters.map { firValueParameter ->
                    firValueParameter.toFirExpression(stubMode)
                }
            }

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
            firEnumEntry.declarations.add(0, firEnumPrimaryConstructor)
        }
    }

    fun List<FirDeclaration>.fillConstructors(
        hasPrimaryConstructor: Boolean,
        delegatedSelfTypeRef: FirTypeRef,
        delegatedSuperTypeRef: FirTypeRef
    ) {
        this.forEach { constructor ->
            (constructor as FirConstructorImpl).returnTypeRef = delegatedSelfTypeRef
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

    fun callableIdForName(name: Name) =
        if (className == FqName.ROOT) CallableId(packageFqName, name)
        else CallableId(packageFqName, className, name)

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


