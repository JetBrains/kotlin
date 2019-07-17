/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.lightTree.fir.TypeConstraint
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStringTemplateExpressionElementType
import org.jetbrains.kotlin.types.Variance

object ConverterUtil {
    private val expressionSet = listOf(
        KtNodeTypes.REFERENCE_EXPRESSION,
        KtNodeTypes.DOT_QUALIFIED_EXPRESSION,
        KtNodeTypes.LAMBDA_EXPRESSION,
        KtNodeTypes.FUN
    )

    fun String?.nameAsSafeName(defaultName: String = ""): Name {
        return when {
            this != null -> Name.identifier(this.replace("`", ""))
            defaultName.isNotEmpty() -> Name.identifier(defaultName)
            else -> SpecialNames.NO_NAME_PROVIDED
        }
    }

    fun LighterASTNode.getAsStringWithoutBacktick(): String {
        return this.toString().replace("`", "")
    }

    fun LighterASTNode.getAsString(): String {
        return this.toString()
    }

    fun LighterASTNode.getAsStringUnescapedValue(): String {
        return this.toString().replaceFirst("\\", "")
    }

    fun LighterASTNode.isExpression(): Boolean {
        return when (this.tokenType) {
            is KtNodeType,
            is KtConstantExpressionElementType,
            is KtStringTemplateExpressionElementType,
            in expressionSet -> true
            else -> false
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
        typeConstraints.forEach { typeConstraint ->
            this.typeParameters.forEach { typeParameter ->
                if (typeConstraint.identifier == typeParameter.name.identifier) {
                    (typeParameter as FirTypeParameterImpl).bounds += typeConstraint.firTypeRef
                    typeParameter.annotations += typeConstraint.firTypeRef.annotations
                    typeParameter.annotations += typeConstraint.annotations
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
        if (!stubMode || this is FirAnnotationCall) {
            this.arguments += container
        }
        return this
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
        get() = ClassId(
            packageFqName,
            className, false
        )

    fun callableIdForName(name: Name, local: Boolean = false) =
        when {
            local -> CallableId(name)
            className == FqName.ROOT -> CallableId(packageFqName, name)
            else -> CallableId(
                packageFqName,
                className, name
            )
        }

    fun callableIdForClassConstructor() =
        if (className == FqName.ROOT) CallableId(packageFqName, Name.special("<anonymous-init>"))
        else CallableId(
            packageFqName,
            className, className.shortName()
        )

    var className: FqName = FqName.ROOT
}

object FunctionUtil {
    val firFunctions = mutableListOf<FirFunction>()
    val firFunctionCalls = mutableListOf<FirFunctionCall>()
    val firLabels = mutableListOf<FirLabel>()

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
        val symbol = FirFunctionSymbol(
            CallableId(
                ClassNameUtil.packageFqName,
                ClassNameUtil.className,
                copyName
            )
        )
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

