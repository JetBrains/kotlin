/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter

typealias FirBasicDeclarationChecker = FirDeclarationChecker<FirDeclaration>
typealias FirCallableDeclarationChecker = FirDeclarationChecker<FirCallableDeclaration>
typealias FirFunctionChecker = FirDeclarationChecker<FirFunction>
typealias FirSimpleFunctionChecker = FirDeclarationChecker<FirSimpleFunction>
typealias FirPropertyChecker = FirDeclarationChecker<FirProperty>
typealias FirClassLikeChecker = FirDeclarationChecker<FirClassLikeDeclaration>
typealias FirClassChecker = FirDeclarationChecker<FirClass>
typealias FirRegularClassChecker = FirDeclarationChecker<FirRegularClass>
typealias FirConstructorChecker = FirDeclarationChecker<FirConstructor>
typealias FirFileChecker = FirDeclarationChecker<FirFile>
typealias FirScriptChecker = FirDeclarationChecker<FirScript>
typealias FirTypeParameterChecker = FirDeclarationChecker<FirTypeParameter>
typealias FirTypeAliasChecker = FirDeclarationChecker<FirTypeAlias>
typealias FirAnonymousFunctionChecker = FirDeclarationChecker<FirAnonymousFunction>
typealias FirPropertyAccessorChecker = FirDeclarationChecker<FirPropertyAccessor>
typealias FirBackingFieldChecker = FirDeclarationChecker<FirBackingField>
typealias FirValueParameterChecker = FirDeclarationChecker<FirValueParameter>
typealias FirEnumEntryChecker = FirDeclarationChecker<FirEnumEntry>
typealias FirAnonymousObjectChecker = FirDeclarationChecker<FirAnonymousObject>
typealias FirAnonymousInitializerChecker = FirDeclarationChecker<FirAnonymousInitializer>
