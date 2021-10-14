/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

abstract class Flow {
    abstract fun getTypeStatement(variable: RealVariable): TypeStatement?
    abstract fun getImplications(variable: DataFlowVariable): Collection<Implication>
    abstract fun getVariablesInTypeStatements(): Collection<RealVariable>
    abstract fun removeOperations(variable: DataFlowVariable): Collection<Implication>

    abstract val directAliasMap: Map<RealVariable, RealVariableAndType>
    abstract val backwardsAliasMap: Map<RealVariable, List<RealVariable>>
    abstract val assignmentIndex: Map<RealVariable, Int>
}

fun Flow.unwrapVariable(variable: RealVariable): RealVariable {
    return directAliasMap[variable]?.variable ?: variable
}
