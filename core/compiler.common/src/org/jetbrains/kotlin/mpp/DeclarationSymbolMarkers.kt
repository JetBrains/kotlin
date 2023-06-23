/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mpp

/*
 * Those markers are needed for implementation of common algorithm of expect/actual
 *   compatibility checking, implemented in
 *   org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
 */
interface DeclarationSymbolMarker
interface CallableSymbolMarker : DeclarationSymbolMarker
interface FunctionSymbolMarker : CallableSymbolMarker
interface ConstructorSymbolMarker : FunctionSymbolMarker
interface SimpleFunctionSymbolMarker : FunctionSymbolMarker
interface PropertySymbolMarker : CallableSymbolMarker
interface ValueParameterSymbolMarker : CallableSymbolMarker
interface FieldSymbolMarker : CallableSymbolMarker
interface EnumEntrySymbolMarker : CallableSymbolMarker

interface ClassifierSymbolMarker : DeclarationSymbolMarker
interface TypeParameterSymbolMarker : ClassifierSymbolMarker
interface ClassLikeSymbolMarker : ClassifierSymbolMarker
interface RegularClassSymbolMarker : ClassLikeSymbolMarker
interface TypeAliasSymbolMarker : ClassLikeSymbolMarker
interface K1SyntheticClassifierSymbolMarker : ClassifierSymbolMarker
