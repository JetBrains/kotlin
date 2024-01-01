/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class DeclarationCheckers {
    companion object {
        val EMPTY: DeclarationCheckers = object : DeclarationCheckers() {}
    }

    open val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = emptySet()
    open val callableDeclarationCheckers: Set<FirCallableDeclarationChecker> = emptySet()
    open val functionCheckers: Set<FirFunctionChecker> = emptySet()
    open val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = emptySet()
    open val propertyCheckers: Set<FirPropertyChecker> = emptySet()
    open val classLikeCheckers: Set<FirClassLikeChecker> = emptySet()
    open val classCheckers: Set<FirClassChecker> = emptySet()
    open val regularClassCheckers: Set<FirRegularClassChecker> = emptySet()
    open val constructorCheckers: Set<FirConstructorChecker> = emptySet()
    open val fileCheckers: Set<FirFileChecker> = emptySet()
    open val scriptCheckers: Set<FirScriptChecker> = emptySet()
    open val typeParameterCheckers: Set<FirTypeParameterChecker> = emptySet()
    open val typeAliasCheckers: Set<FirTypeAliasChecker> = emptySet()
    open val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker> = emptySet()
    open val propertyAccessorCheckers: Set<FirPropertyAccessorChecker> = emptySet()
    open val backingFieldCheckers: Set<FirBackingFieldChecker> = emptySet()
    open val valueParameterCheckers: Set<FirValueParameterChecker> = emptySet()
    open val enumEntryCheckers: Set<FirEnumEntryChecker> = emptySet()
    open val anonymousObjectCheckers: Set<FirAnonymousObjectChecker> = emptySet()
    open val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker> = emptySet()

    open val controlFlowAnalyserCheckers: Set<FirControlFlowChecker> = emptySet()
    open val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicDeclarationCheckers: Set<FirBasicDeclarationChecker> by lazy { basicDeclarationCheckers }
    @CheckersComponentInternal internal val allCallableDeclarationCheckers: Set<FirCallableDeclarationChecker> by lazy { callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allFunctionCheckers: Set<FirFunctionChecker> by lazy { functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allSimpleFunctionCheckers: Set<FirSimpleFunctionChecker> by lazy { simpleFunctionCheckers + functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allPropertyCheckers: Set<FirPropertyChecker> by lazy { propertyCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allClassLikeCheckers: Set<FirClassLikeChecker> by lazy { classLikeCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allClassCheckers: Set<FirClassChecker> by lazy { classCheckers + classLikeCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allRegularClassCheckers: Set<FirRegularClassChecker> by lazy { regularClassCheckers + classCheckers + classLikeCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allConstructorCheckers: Set<FirConstructorChecker> by lazy { constructorCheckers + functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allFileCheckers: Set<FirFileChecker> by lazy { fileCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allScriptCheckers: Set<FirScriptChecker> by lazy { scriptCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allTypeParameterCheckers: Set<FirTypeParameterChecker> by lazy { typeParameterCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allTypeAliasCheckers: Set<FirTypeAliasChecker> by lazy { typeAliasCheckers + classLikeCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allAnonymousFunctionCheckers: Set<FirAnonymousFunctionChecker> by lazy { anonymousFunctionCheckers + functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allPropertyAccessorCheckers: Set<FirPropertyAccessorChecker> by lazy { propertyAccessorCheckers + functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allBackingFieldCheckers: Set<FirBackingFieldChecker> by lazy { backingFieldCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allValueParameterCheckers: Set<FirValueParameterChecker> by lazy { valueParameterCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allEnumEntryCheckers: Set<FirEnumEntryChecker> by lazy { enumEntryCheckers + callableDeclarationCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allAnonymousObjectCheckers: Set<FirAnonymousObjectChecker> by lazy { anonymousObjectCheckers + classCheckers + classLikeCheckers + basicDeclarationCheckers }
    @CheckersComponentInternal internal val allAnonymousInitializerCheckers: Set<FirAnonymousInitializerChecker> by lazy { anonymousInitializerCheckers + basicDeclarationCheckers }
}
