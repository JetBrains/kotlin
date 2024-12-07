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
    open val replSnippetCheckers: Set<FirReplSnippetChecker> = emptySet()
    open val typeParameterCheckers: Set<FirTypeParameterChecker> = emptySet()
    open val typeAliasCheckers: Set<FirTypeAliasChecker> = emptySet()
    open val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker> = emptySet()
    open val propertyAccessorCheckers: Set<FirPropertyAccessorChecker> = emptySet()
    open val backingFieldCheckers: Set<FirBackingFieldChecker> = emptySet()
    open val valueParameterCheckers: Set<FirValueParameterChecker> = emptySet()
    open val enumEntryCheckers: Set<FirEnumEntryChecker> = emptySet()
    open val anonymousObjectCheckers: Set<FirAnonymousObjectChecker> = emptySet()
    open val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker> = emptySet()
    open val receiverParameterCheckers: Set<FirReceiverParameterChecker> = emptySet()

    open val controlFlowAnalyserCheckers: Set<FirControlFlowChecker> = emptySet()
    open val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicDeclarationCheckers: Array<FirBasicDeclarationChecker> by lazy { basicDeclarationCheckers.toTypedArray() }
    @CheckersComponentInternal internal val allCallableDeclarationCheckers: Array<FirCallableDeclarationChecker> by lazy { (callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allFunctionCheckers: Array<FirFunctionChecker> by lazy { (functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allSimpleFunctionCheckers: Array<FirSimpleFunctionChecker> by lazy { (simpleFunctionCheckers + functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allPropertyCheckers: Array<FirPropertyChecker> by lazy { (propertyCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allClassLikeCheckers: Array<FirClassLikeChecker> by lazy { (classLikeCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allClassCheckers: Array<FirClassChecker> by lazy { (classCheckers + classLikeCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allRegularClassCheckers: Array<FirRegularClassChecker> by lazy { (regularClassCheckers + classCheckers + classLikeCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allConstructorCheckers: Array<FirConstructorChecker> by lazy { (constructorCheckers + functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allFileCheckers: Array<FirFileChecker> by lazy { (fileCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allScriptCheckers: Array<FirScriptChecker> by lazy { (scriptCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allReplSnippetCheckers: Array<FirReplSnippetChecker> by lazy { (replSnippetCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allTypeParameterCheckers: Array<FirTypeParameterChecker> by lazy { (typeParameterCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allTypeAliasCheckers: Array<FirTypeAliasChecker> by lazy { (typeAliasCheckers + classLikeCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allAnonymousFunctionCheckers: Array<FirAnonymousFunctionChecker> by lazy { (anonymousFunctionCheckers + functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allPropertyAccessorCheckers: Array<FirPropertyAccessorChecker> by lazy { (propertyAccessorCheckers + functionCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allBackingFieldCheckers: Array<FirBackingFieldChecker> by lazy { (backingFieldCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allValueParameterCheckers: Array<FirValueParameterChecker> by lazy { (valueParameterCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allEnumEntryCheckers: Array<FirEnumEntryChecker> by lazy { (enumEntryCheckers + callableDeclarationCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allAnonymousObjectCheckers: Array<FirAnonymousObjectChecker> by lazy { (anonymousObjectCheckers + classCheckers + classLikeCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allAnonymousInitializerCheckers: Array<FirAnonymousInitializerChecker> by lazy { (anonymousInitializerCheckers + basicDeclarationCheckers).toTypedArray() }
    @CheckersComponentInternal internal val allReceiverParameterCheckers: Array<FirReceiverParameterChecker> by lazy { (receiverParameterCheckers + basicDeclarationCheckers).toTypedArray() }
}
