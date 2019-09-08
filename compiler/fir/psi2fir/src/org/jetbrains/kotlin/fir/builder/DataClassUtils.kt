/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.FirImplicitThisReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter

fun List<Pair<KtParameter?, FirProperty>>.generateComponentFunctions(
    session: FirSession, firClass: FirClassImpl, packageFqName: FqName, classFqName: FqName,
    firPrimaryConstructor: FirConstructor
) {
    var componentIndex = 1
    for ((ktParameter, firProperty) in this) {
        if (!firProperty.isVal && !firProperty.isVar) continue
        val name = Name.identifier("component$componentIndex")
        componentIndex++
        val symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, name))
        firClass.addDeclaration(
            FirMemberFunctionImpl(
                session, ktParameter, symbol, name,
                Visibilities.PUBLIC, Modality.FINAL,
                isExpect = false, isActual = false,
                isOverride = false, isOperator = false,
                isInfix = false, isInline = false,
                isTailRec = false, isExternal = false,
                isSuspend = false, receiverTypeRef = null,
                returnTypeRef = FirImplicitTypeRefImpl(ktParameter)
            ).apply {
                val componentFunction = this
                body = FirSingleExpressionBlock(
                    FirReturnExpressionImpl(
                        ktParameter,
                        FirQualifiedAccessExpressionImpl(ktParameter).apply {
                            val parameterName = firProperty.name
                            dispatchReceiver = FirThisReceiverExpressionImpl(null, FirImplicitThisReference(firClass.symbol)).apply {
                                typeRef = firPrimaryConstructor.returnTypeRef
                            }
                            calleeReference = FirResolvedCallableReferenceImpl(
                                ktParameter,
                                parameterName, firProperty.symbol
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

fun List<Pair<KtParameter?, FirProperty>>.generateCopyFunction(
    session: FirSession, classOrObject: KtClassOrObject?, firClass: FirClassImpl, packageFqName: FqName, classFqName: FqName,
    firPrimaryConstructor: FirConstructor
) {
    val symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, copyName))
    firClass.addDeclaration(
        FirMemberFunctionImpl(
            session, classOrObject, symbol, copyName,
            Visibilities.PUBLIC, Modality.FINAL,
            isExpect = false, isActual = false,
            isOverride = false, isOperator = false,
            isInfix = false, isInline = false,
            isTailRec = false, isExternal = false,
            isSuspend = false, receiverTypeRef = null,
            returnTypeRef = firPrimaryConstructor.returnTypeRef
        ).apply {
            for ((ktParameter, firProperty) in this@generateCopyFunction) {
                val name = firProperty.name
                valueParameters += FirValueParameterImpl(
                    session, ktParameter, name,
                    firProperty.returnTypeRef,
                    FirQualifiedAccessExpressionImpl(ktParameter).apply {
                        dispatchReceiver = FirThisReceiverExpressionImpl(null, FirImplicitThisReference(firClass.symbol)).apply {
                            typeRef = firPrimaryConstructor.returnTypeRef
                        }
                        calleeReference = FirResolvedCallableReferenceImpl(ktParameter, name, firProperty.symbol)
                    },
                    isCrossinline = false, isNoinline = false, isVararg = false
                )
            }

            body = FirEmptyExpressionBlock()
//            body = FirSingleExpressionBlock(
//                session,
//                FirReturnExpressionImpl(
//                    session, this@generateCopyFunction,
//                    FirFunctionCallImpl(session, this@generateCopyFunction).apply {
//                        calleeReference = FirResolvedCallableReferenceImpl(
//                            session, this@generateCopyFunction, firClass.name,
//                            firPrimaryConstructor.symbol
//                        )
//                    }.apply {
//                        for ((ktParameter, firParameter) in primaryConstructorParameters.zip(valueParameters)) {
//                            this.arguments += FirQualifiedAccessExpressionImpl(session, ktParameter).apply {
//                                calleeReference = FirResolvedCallableReferenceImpl(
//                                    session, ktParameter, firParameter.name, firParameter.symbol
//                                )
//                            }
//                        }
//                    }
//                ).apply {
//                    target = FirFunctionTarget(null)
//                    target.bind(copyFunction)
//                }
//            )
        }
    )
}