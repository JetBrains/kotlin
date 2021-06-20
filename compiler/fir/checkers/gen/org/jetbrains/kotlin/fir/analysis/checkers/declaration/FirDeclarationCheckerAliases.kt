/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter

typealias FirBasicDeclarationChecker = FirDeclarationChecker<FirDeclaration<*>>
typealias FirMemberDeclarationChecker = FirDeclarationChecker<FirMemberDeclaration<*>>
typealias FirFunctionChecker = FirDeclarationChecker<FirFunction<*>>
typealias FirSimpleFunctionChecker = FirDeclarationChecker<FirSimpleFunction>
typealias FirPropertyChecker = FirDeclarationChecker<FirProperty>
typealias FirClassChecker = FirDeclarationChecker<FirClass<*>>
typealias FirRegularClassChecker = FirDeclarationChecker<FirRegularClass>
typealias FirConstructorChecker = FirDeclarationChecker<FirConstructor>
typealias FirFileChecker = FirDeclarationChecker<FirFile>
typealias FirTypeParameterChecker = FirDeclarationChecker<FirTypeParameter>
typealias FirAnnotatedDeclarationChecker = FirDeclarationChecker<FirAnnotatedDeclaration<*>>
