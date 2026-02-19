/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.ClassId

fun FirEnumEntryDeserializedAccessExpression.toQualifiedPropertyAccessExpression(session: FirSession): FirPropertyAccessExpression =
    buildPropertyAccessExpression {
        val entryPropertySymbol = session.getClassDeclaredPropertySymbols(
            enumClassId, enumEntryName,
        ).firstOrNull { it.isStatic }

        calleeReference = when {
            entryPropertySymbol != null -> {
                buildResolvedNamedReference {
                    this.name = enumEntryName
                    resolvedSymbol = entryPropertySymbol
                }
            }
            else -> {
                buildErrorNamedReference {
                    diagnostic = ConeSimpleDiagnostic(
                        "Strange deserialized enum value: $enumClassId.$enumEntryName",
                        DiagnosticKind.Java,
                    )
                    name = enumEntryName
                }
            }
        }

        val receiver = enumClassId.toResolvedQualifier(session)
        coneTypeOrNull = receiver.resolvedType
        dispatchReceiver = receiver
        explicitReceiver = receiver
    }

fun ClassId.toResolvedQualifier(session: FirSession): FirResolvedQualifier {
    val lookupTag = toLookupTag()

    return buildResolvedQualifier {
        coneTypeOrNull = lookupTag.constructClassType()
        packageFqName = this@toResolvedQualifier.packageFqName
        relativeClassFqName = relativeClassName
        symbol = lookupTag.toSymbol(session)
        resolvedToCompanionObject = false
    }
}
