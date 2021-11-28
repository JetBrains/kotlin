/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

class ComposedDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = _basicDeclarationCheckers
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
    private val _functionCheckers: MutableSet<FirFunctionChecker> = mutableSetOf()
    private val _simpleFunctionCheckers: MutableSet<FirSimpleFunctionChecker> = mutableSetOf()
    private val _propertyCheckers: MutableSet<FirPropertyChecker> = mutableSetOf()
    private val _classLikeCheckers: MutableSet<FirClassLikeChecker> = mutableSetOf()
    private val _classCheckers: MutableSet<FirClassChecker> = mutableSetOf()
    private val _regularClassCheckers: MutableSet<FirRegularClassChecker> = mutableSetOf()
    private val _constructorCheckers: MutableSet<FirConstructorChecker> = mutableSetOf()
    private val _fileCheckers: MutableSet<FirFileChecker> = mutableSetOf()
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
        _basicDeclarationCheckers += checkers.basicDeclarationCheckers
        _functionCheckers += checkers.functionCheckers
        _simpleFunctionCheckers += checkers.simpleFunctionCheckers
        _propertyCheckers += checkers.propertyCheckers
        _classLikeCheckers += checkers.classLikeCheckers
        _classCheckers += checkers.classCheckers
        _regularClassCheckers += checkers.regularClassCheckers
        _constructorCheckers += checkers.constructorCheckers
        _fileCheckers += checkers.fileCheckers
        _typeParameterCheckers += checkers.typeParameterCheckers
        _typeAliasCheckers += checkers.typeAliasCheckers
        _anonymousFunctionCheckers += checkers.anonymousFunctionCheckers
        _propertyAccessorCheckers += checkers.propertyAccessorCheckers
        _backingFieldCheckers += checkers.backingFieldCheckers
        _valueParameterCheckers += checkers.valueParameterCheckers
        _enumEntryCheckers += checkers.enumEntryCheckers
        _anonymousObjectCheckers += checkers.anonymousObjectCheckers
        _anonymousInitializerCheckers += checkers.anonymousInitializerCheckers
        _controlFlowAnalyserCheckers += checkers.controlFlowAnalyserCheckers
        _variableAssignmentCfaBasedCheckers += checkers.variableAssignmentCfaBasedCheckers
    }
}
