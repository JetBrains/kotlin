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

class CompositeDeclarationCheckers(val predicate: (FirCheckerWithMppKind) -> Boolean) : DeclarationCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        field: MutableSet<FirBasicDeclarationChecker> = []
    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker>
        field: MutableSet<FirCallableDeclarationChecker> = []
    override val functionCheckers: Set<FirFunctionChecker>
        field: MutableSet<FirFunctionChecker> = []
    override val namedFunctionCheckers: Set<FirNamedFunctionChecker>
        field: MutableSet<FirNamedFunctionChecker> = []
    override val propertyCheckers: Set<FirPropertyChecker>
        field: MutableSet<FirPropertyChecker> = []
    override val classLikeCheckers: Set<FirClassLikeChecker>
        field: MutableSet<FirClassLikeChecker> = []
    override val classCheckers: Set<FirClassChecker>
        field: MutableSet<FirClassChecker> = []
    override val regularClassCheckers: Set<FirRegularClassChecker>
        field: MutableSet<FirRegularClassChecker> = []
    override val constructorCheckers: Set<FirConstructorChecker>
        field: MutableSet<FirConstructorChecker> = []
    override val fileCheckers: Set<FirFileChecker>
        field: MutableSet<FirFileChecker> = []
    override val scriptCheckers: Set<FirScriptChecker>
        field: MutableSet<FirScriptChecker> = []
    override val replSnippetCheckers: Set<FirReplSnippetChecker>
        field: MutableSet<FirReplSnippetChecker> = []
    override val typeParameterCheckers: Set<FirTypeParameterChecker>
        field: MutableSet<FirTypeParameterChecker> = []
    override val typeAliasCheckers: Set<FirTypeAliasChecker>
        field: MutableSet<FirTypeAliasChecker> = []
    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker>
        field: MutableSet<FirAnonymousFunctionChecker> = []
    override val propertyAccessorCheckers: Set<FirPropertyAccessorChecker>
        field: MutableSet<FirPropertyAccessorChecker> = []
    override val backingFieldCheckers: Set<FirBackingFieldChecker>
        field: MutableSet<FirBackingFieldChecker> = []
    override val valueParameterCheckers: Set<FirValueParameterChecker>
        field: MutableSet<FirValueParameterChecker> = []
    override val enumEntryCheckers: Set<FirEnumEntryChecker>
        field: MutableSet<FirEnumEntryChecker> = []
    override val anonymousObjectCheckers: Set<FirAnonymousObjectChecker>
        field: MutableSet<FirAnonymousObjectChecker> = []
    override val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker>
        field: MutableSet<FirAnonymousInitializerChecker> = []
    override val receiverParameterCheckers: Set<FirReceiverParameterChecker>
        field: MutableSet<FirReceiverParameterChecker> = []
    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker>
        field: MutableSet<FirControlFlowChecker> = []
    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker>
        field: MutableSet<AbstractFirPropertyInitializationChecker> = []

    @CheckersComponentInternal
    fun register(checkers: DeclarationCheckers) {
        checkers.basicDeclarationCheckers.filterTo(basicDeclarationCheckers, predicate)
        checkers.callableDeclarationCheckers.filterTo(callableDeclarationCheckers, predicate)
        checkers.functionCheckers.filterTo(functionCheckers, predicate)
        checkers.namedFunctionCheckers.filterTo(namedFunctionCheckers, predicate)
        checkers.propertyCheckers.filterTo(propertyCheckers, predicate)
        checkers.classLikeCheckers.filterTo(classLikeCheckers, predicate)
        checkers.classCheckers.filterTo(classCheckers, predicate)
        checkers.regularClassCheckers.filterTo(regularClassCheckers, predicate)
        checkers.constructorCheckers.filterTo(constructorCheckers, predicate)
        checkers.fileCheckers.filterTo(fileCheckers, predicate)
        checkers.scriptCheckers.filterTo(scriptCheckers, predicate)
        checkers.replSnippetCheckers.filterTo(replSnippetCheckers, predicate)
        checkers.typeParameterCheckers.filterTo(typeParameterCheckers, predicate)
        checkers.typeAliasCheckers.filterTo(typeAliasCheckers, predicate)
        checkers.anonymousFunctionCheckers.filterTo(anonymousFunctionCheckers, predicate)
        checkers.propertyAccessorCheckers.filterTo(propertyAccessorCheckers, predicate)
        checkers.backingFieldCheckers.filterTo(backingFieldCheckers, predicate)
        checkers.valueParameterCheckers.filterTo(valueParameterCheckers, predicate)
        checkers.enumEntryCheckers.filterTo(enumEntryCheckers, predicate)
        checkers.anonymousObjectCheckers.filterTo(anonymousObjectCheckers, predicate)
        checkers.anonymousInitializerCheckers.filterTo(anonymousInitializerCheckers, predicate)
        checkers.receiverParameterCheckers.filterTo(receiverParameterCheckers, predicate)
        checkers.controlFlowAnalyserCheckers.filterTo(controlFlowAnalyserCheckers, predicate)
        checkers.variableAssignmentCfaBasedCheckers.filterTo(variableAssignmentCfaBasedCheckers, predicate)
    }
}
