/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FilteredDeclarationCheckers(
    val delegate: DeclarationCheckers,
    val predicate: (FirDeclarationChecker<*>) -> Boolean
) : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = delegate.basicDeclarationCheckers.filterTo(mutableSetOf(), predicate)
    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker> = delegate.callableDeclarationCheckers.filterTo(mutableSetOf(), predicate)
    override val functionCheckers: Set<FirFunctionChecker> = delegate.functionCheckers.filterTo(mutableSetOf(), predicate)
    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = delegate.simpleFunctionCheckers.filterTo(mutableSetOf(), predicate)
    override val propertyCheckers: Set<FirPropertyChecker> = delegate.propertyCheckers.filterTo(mutableSetOf(), predicate)
    override val classLikeCheckers: Set<FirClassLikeChecker> = delegate.classLikeCheckers.filterTo(mutableSetOf(), predicate)
    override val classCheckers: Set<FirClassChecker> = delegate.classCheckers.filterTo(mutableSetOf(), predicate)
    override val regularClassCheckers: Set<FirRegularClassChecker> = delegate.regularClassCheckers.filterTo(mutableSetOf(), predicate)
    override val constructorCheckers: Set<FirConstructorChecker> = delegate.constructorCheckers.filterTo(mutableSetOf(), predicate)
    override val fileCheckers: Set<FirFileChecker> = delegate.fileCheckers.filterTo(mutableSetOf(), predicate)
    override val scriptCheckers: Set<FirScriptChecker> = delegate.scriptCheckers.filterTo(mutableSetOf(), predicate)
    override val replSnippetCheckers: Set<FirReplSnippetChecker> = delegate.replSnippetCheckers.filterTo(mutableSetOf(), predicate)
    override val typeParameterCheckers: Set<FirTypeParameterChecker> = delegate.typeParameterCheckers.filterTo(mutableSetOf(), predicate)
    override val typeAliasCheckers: Set<FirTypeAliasChecker> = delegate.typeAliasCheckers.filterTo(mutableSetOf(), predicate)
    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker> = delegate.anonymousFunctionCheckers.filterTo(mutableSetOf(), predicate)
    override val propertyAccessorCheckers: Set<FirPropertyAccessorChecker> = delegate.propertyAccessorCheckers.filterTo(mutableSetOf(), predicate)
    override val backingFieldCheckers: Set<FirBackingFieldChecker> = delegate.backingFieldCheckers.filterTo(mutableSetOf(), predicate)
    override val valueParameterCheckers: Set<FirValueParameterChecker> = delegate.valueParameterCheckers.filterTo(mutableSetOf(), predicate)
    override val enumEntryCheckers: Set<FirEnumEntryChecker> = delegate.enumEntryCheckers.filterTo(mutableSetOf(), predicate)
    override val anonymousObjectCheckers: Set<FirAnonymousObjectChecker> = delegate.anonymousObjectCheckers.filterTo(mutableSetOf(), predicate)
    override val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker> = delegate.anonymousInitializerCheckers.filterTo(mutableSetOf(), predicate)
    override val receiverParameterCheckers: Set<FirReceiverParameterChecker> = delegate.receiverParameterCheckers.filterTo(mutableSetOf(), predicate)
}
