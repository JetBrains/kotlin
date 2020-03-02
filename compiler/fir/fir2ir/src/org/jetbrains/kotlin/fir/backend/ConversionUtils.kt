/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.psi.PsiCompiledElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.Variance

internal fun <T : IrElement> FirElement.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    if (psi is PsiCompiledElement) return f(-1, -1)
    val startOffset = psi?.startOffsetSkippingComments ?: -1
    val endOffset = psi?.endOffset ?: -1
    return f(startOffset, endOffset)
}

internal fun createErrorType(): IrErrorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)

internal enum class ConversionTypeOrigin {
    DEFAULT,
    SETTER
}

class ConversionTypeContext internal constructor(
    internal val definitelyNotNull: Boolean,
    internal val origin: ConversionTypeOrigin
) {
    fun definitelyNotNull() = ConversionTypeContext(true, origin)

    fun inSetter() = ConversionTypeContext(definitelyNotNull, ConversionTypeOrigin.SETTER)

    companion object {
        internal val DEFAULT = ConversionTypeContext(
            definitelyNotNull = false, origin = ConversionTypeOrigin.DEFAULT
        )
    }
}

fun FirClassifierSymbol<*>.toIrSymbol(
    session: FirSession,
    declarationStorage: Fir2IrDeclarationStorage,
    typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
): IrClassifierSymbol {
    return when (this) {
        is FirTypeParameterSymbol -> {
            toTypeParameterSymbol(declarationStorage, typeContext)
        }
        is FirTypeAliasSymbol -> {
            val typeAlias = fir
            val coneClassLikeType = (typeAlias.expandedTypeRef as FirResolvedTypeRef).type as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)!!.toIrSymbol(session, declarationStorage)
        }
        is FirClassSymbol -> {
            toClassSymbol(declarationStorage)
        }
        else -> throw AssertionError("Should not be here: $this")
    }
}

fun FirReference.toSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol? {
    return when (this) {
        is FirResolvedNamedReference -> {
            when (val resolvedSymbol = resolvedSymbol) {
                is FirCallableSymbol<*> -> {
                    val originalCallableSymbol =
                        resolvedSymbol.overriddenSymbol?.takeIf { it.callableId == resolvedSymbol.callableId } ?: resolvedSymbol
                    originalCallableSymbol.toSymbol(declarationStorage)
                }
                else -> {
                    resolvedSymbol.toSymbol(declarationStorage)
                }
            }
        }
        is FirThisReference -> {
            when (val boundSymbol = boundSymbol?.toSymbol(declarationStorage)) {
                is IrClassSymbol -> boundSymbol.owner.thisReceiver?.symbol
                is IrFunctionSymbol -> boundSymbol.owner.extensionReceiverParameter?.symbol
                else -> null
            }
        }
        else -> null
    }
}

private fun AbstractFirBasedSymbol<*>.toSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol? = when (this) {
    is FirClassSymbol -> toClassSymbol(declarationStorage)
    is FirFunctionSymbol<*> -> toFunctionSymbol(declarationStorage)
    is FirPropertySymbol -> if (fir.isLocal) toValueSymbol(declarationStorage) else toPropertyOrFieldSymbol(declarationStorage)
    is FirFieldSymbol -> toPropertyOrFieldSymbol(declarationStorage)
    is FirBackingFieldSymbol -> toBackingFieldSymbol(declarationStorage)
    is FirDelegateFieldSymbol<*> -> toBackingFieldSymbol(declarationStorage)
    is FirVariableSymbol<*> -> toValueSymbol(declarationStorage)
    else -> null
}

fun FirClassSymbol<*>.toClassSymbol(declarationStorage: Fir2IrDeclarationStorage): IrClassSymbol {
    return declarationStorage.getIrClassSymbol(this)
}

fun FirTypeParameterSymbol.toTypeParameterSymbol(
    declarationStorage: Fir2IrDeclarationStorage,
    typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
): IrTypeParameterSymbol {
    return declarationStorage.getIrTypeParameterSymbol(this, typeContext)
}

fun FirFunctionSymbol<*>.toFunctionSymbol(declarationStorage: Fir2IrDeclarationStorage): IrFunctionSymbol {
    return declarationStorage.getIrFunctionSymbol(this)
}

fun FirVariableSymbol<*>.toPropertyOrFieldSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol {
    return declarationStorage.getIrPropertyOrFieldSymbol(this)
}

fun FirVariableSymbol<*>.toBackingFieldSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol {
    return declarationStorage.getIrBackingFieldSymbol(this)
}

fun FirVariableSymbol<*>.toValueSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol {
    return declarationStorage.getIrValueSymbol(this)
}

fun FirConstExpression<*>.getIrConstKind(): IrConstKind<*> = when (kind) {
    FirConstKind.IntegerLiteral -> {
        val type = typeRef.coneTypeUnsafe<ConeIntegerLiteralType>()
        type.getApproximatedType().toConstKind()!!.toIrConstKind()
    }
    else -> kind.toIrConstKind()
}

private fun FirConstKind<*>.toIrConstKind(): IrConstKind<*> = when (this) {
    FirConstKind.Null -> IrConstKind.Null
    FirConstKind.Boolean -> IrConstKind.Boolean
    FirConstKind.Char -> IrConstKind.Char
    FirConstKind.Byte -> IrConstKind.Byte
    FirConstKind.Short -> IrConstKind.Short
    FirConstKind.Int -> IrConstKind.Int
    FirConstKind.Long -> IrConstKind.Long
    FirConstKind.String -> IrConstKind.String
    FirConstKind.Float -> IrConstKind.Float
    FirConstKind.Double -> IrConstKind.Double
    FirConstKind.IntegerLiteral -> throw IllegalArgumentException()
}

internal fun FirClass<*>.collectCallableNamesFromSupertypes(session: FirSession, result: MutableList<Name> = mutableListOf()): List<Name> {
    for (superTypeRef in superTypeRefs) {
        superTypeRef.collectCallableNamesFromThisAndSupertypes(session, result)
    }
    return result
}

private fun FirTypeRef.collectCallableNamesFromThisAndSupertypes(
    session: FirSession,
    result: MutableList<Name> = mutableListOf()
): List<Name> {
    if (this is FirResolvedTypeRef) {
        val superType = type
        if (superType is ConeClassLikeType) {
            when (val superSymbol = superType.lookupTag.toSymbol(session)) {
                is FirClassSymbol -> {
                    val superClass = superSymbol.fir as FirClass<*>
                    for (declaration in superClass.declarations) {
                        when (declaration) {
                            is FirSimpleFunction -> result += declaration.name
                            is FirVariable<*> -> result += declaration.name
                        }
                    }
                    superClass.collectCallableNamesFromSupertypes(session, result)
                }
                is FirTypeAliasSymbol -> {
                    val superAlias = superSymbol.fir
                    superAlias.expandedTypeRef.collectCallableNamesFromThisAndSupertypes(session, result)
                }
            }
        }
    }
    return result
}

internal tailrec fun FirCallableSymbol<*>.deepestOverriddenSymbol(): FirCallableSymbol<*> {
    val overriddenSymbol = overriddenSymbol ?: return this
    return overriddenSymbol.deepestOverriddenSymbol()
}