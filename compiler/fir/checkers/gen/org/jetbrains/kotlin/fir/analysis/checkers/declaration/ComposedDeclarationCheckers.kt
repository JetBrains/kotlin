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

class ComposedDeclarationCheckers(val predicate: (FirCheckerWithMppKind) -> Boolean) : DeclarationCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = _basicDeclarationCheckers
    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker>
        get() = _callableDeclarationCheckers
    override val functionCheckers: Set<FirFunctionChecker>
        get() = _functionCheckers
    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
        get() = _simpleFunctionCheckers
    override val propertyCheckers: Set<FirPropertyChecker>
        get() = _propertyCheckers
    override val classLikeCheckers: Set<FirClassLikeChecker>
        get() = _classLikeCheckers
    override val classCheckers: Set<FirClassChecker>
        get() = _classCheckers
    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = _regularClassCheckers
    override val constructorCheckers: Set<FirConstructorChecker>
        get() = _constructorCheckers
    override val fileCheckers: Set<FirFileChecker>
        get() = _fileCheckers
    override val scriptCheckers: Set<FirScriptChecker>
        get() = _scriptCheckers
    override val typeParameterCheckers: Set<FirTypeParameterChecker>
        get() = _typeParameterCheckers
    override val typeAliasCheckers: Set<FirTypeAliasChecker>
        get() = _typeAliasCheckers
    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker>
        get() = _anonymousFunctionCheckers
    override val propertyAccessorCheckers: Set<FirPropertyAccessorChecker>
        get() = _propertyAccessorCheckers
    override val backingFieldCheckers: Set<FirBackingFieldChecker>
        get() = _backingFieldCheckers
    override val valueParameterCheckers: Set<FirValueParameterChecker>
        get() = _valueParameterCheckers
    override val enumEntryCheckers: Set<FirEnumEntryChecker>
        get() = _enumEntryCheckers
    override val anonymousObjectCheckers: Set<FirAnonymousObjectChecker>
        get() = _anonymousObjectCheckers
    override val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker>
        get() = _anonymousInitializerCheckers
    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker>
        get() = _controlFlowAnalyserCheckers
    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker>
        get() = _variableAssignmentCfaBasedCheckers

    private val _basicDeclarationCheckers: MutableSet<FirBasicDeclarationChecker> = mutableSetOf()
    private val _callableDeclarationCheckers: MutableSet<FirCallableDeclarationChecker> = mutableSetOf()
    private val _functionCheckers: MutableSet<FirFunctionChecker> = mutableSetOf()
    private val _simpleFunctionCheckers: MutableSet<FirSimpleFunctionChecker> = mutableSetOf()
    private val _propertyCheckers: MutableSet<FirPropertyChecker> = mutableSetOf()
    private val _classLikeCheckers: MutableSet<FirClassLikeChecker> = mutableSetOf()
    private val _classCheckers: MutableSet<FirClassChecker> = mutableSetOf()
    private val _regularClassCheckers: MutableSet<FirRegularClassChecker> = mutableSetOf()
    private val _constructorCheckers: MutableSet<FirConstructorChecker> = mutableSetOf()
    private val _fileCheckers: MutableSet<FirFileChecker> = mutableSetOf()
    private val _scriptCheckers: MutableSet<FirScriptChecker> = mutableSetOf()
    private val _typeParameterCheckers: MutableSet<FirTypeParameterChecker> = mutableSetOf()
    private val _typeAliasCheckers: MutableSet<FirTypeAliasChecker> = mutableSetOf()
    private val _anonymousFunctionCheckers: MutableSet<FirAnonymousFunctionChecker> = mutableSetOf()
    private val _propertyAccessorCheckers: MutableSet<FirPropertyAccessorChecker> = mutableSetOf()
    private val _backingFieldCheckers: MutableSet<FirBackingFieldChecker> = mutableSetOf()
    private val _valueParameterCheckers: MutableSet<FirValueParameterChecker> = mutableSetOf()
    private val _enumEntryCheckers: MutableSet<FirEnumEntryChecker> = mutableSetOf()
    private val _anonymousObjectCheckers: MutableSet<FirAnonymousObjectChecker> = mutableSetOf()
    private val _anonymousInitializerCheckers: MutableSet<FirAnonymousInitializerChecker> = mutableSetOf()
    private val _controlFlowAnalyserCheckers: MutableSet<FirControlFlowChecker> = mutableSetOf()
    private val _variableAssignmentCfaBasedCheckers: MutableSet<AbstractFirPropertyInitializationChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: DeclarationCheckers) {
        checkers.basicDeclarationCheckers.filterTo(_basicDeclarationCheckers, predicate)
        checkers.callableDeclarationCheckers.filterTo(_callableDeclarationCheckers, predicate)
        checkers.functionCheckers.filterTo(_functionCheckers, predicate)
        checkers.simpleFunctionCheckers.filterTo(_simpleFunctionCheckers, predicate)
        checkers.propertyCheckers.filterTo(_propertyCheckers, predicate)
        checkers.classLikeCheckers.filterTo(_classLikeCheckers, predicate)
        checkers.classCheckers.filterTo(_classCheckers, predicate)
        checkers.regularClassCheckers.filterTo(_regularClassCheckers, predicate)
        checkers.constructorCheckers.filterTo(_constructorCheckers, predicate)
        checkers.fileCheckers.filterTo(_fileCheckers, predicate)
        checkers.scriptCheckers.filterTo(_scriptCheckers, predicate)
        checkers.typeParameterCheckers.filterTo(_typeParameterCheckers, predicate)
        checkers.typeAliasCheckers.filterTo(_typeAliasCheckers, predicate)
        checkers.anonymousFunctionCheckers.filterTo(_anonymousFunctionCheckers, predicate)
        checkers.propertyAccessorCheckers.filterTo(_propertyAccessorCheckers, predicate)
        checkers.backingFieldCheckers.filterTo(_backingFieldCheckers, predicate)
        checkers.valueParameterCheckers.filterTo(_valueParameterCheckers, predicate)
        checkers.enumEntryCheckers.filterTo(_enumEntryCheckers, predicate)
        checkers.anonymousObjectCheckers.filterTo(_anonymousObjectCheckers, predicate)
        checkers.anonymousInitializerCheckers.filterTo(_anonymousInitializerCheckers, predicate)
        checkers.controlFlowAnalyserCheckers.filterTo(_controlFlowAnalyserCheckers, predicate)
        checkers.variableAssignmentCfaBasedCheckers.filterTo(_variableAssignmentCfaBasedCheckers, predicate)
    }
}
