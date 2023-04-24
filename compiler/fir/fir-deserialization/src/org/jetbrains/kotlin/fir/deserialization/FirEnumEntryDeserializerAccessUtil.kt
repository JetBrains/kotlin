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
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag

fun FirEnumEntryDeserializedAccessExpression.toQualifiedPropertyAccessExpression(session: FirSession): FirPropertyAccessExpression =
    buildPropertyAccessExpression {
        val entryPropertySymbol = session.symbolProvider.getClassDeclaredPropertySymbols(
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
                }
            }
        }

        typeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                enumClassId.toLookupTag(), emptyArray(), isNullable = false
            )
        }
    }
