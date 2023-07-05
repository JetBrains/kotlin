/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirContractsDslNames {
    // Types
    val SIMPLE_EFFECT = id("SimpleEffect")

    // Structure-defining calls
    val CONTRACT = id("contract")
    val IMPLIES = simpleEffect("implies")

    // Effect-declaration calls
    val RETURNS = contractBuilder("returns")
    val RETURNS_NOT_NULL = contractBuilder("returnsNotNull")
    val CALLS_IN_PLACE = contractBuilder("callsInPlace")

    // enum class InvocationKind
    val INVOCATION_KIND_ENUM = id("InvocationKind")
    val EXACTLY_ONCE_KIND = invocationKind("EXACTLY_ONCE")
    val AT_LEAST_ONCE_KIND = invocationKind("AT_LEAST_ONCE")
    val UNKNOWN_KIND = invocationKind("UNKNOWN")
    val AT_MOST_ONCE_KIND = invocationKind("AT_MOST_ONCE")

    private const val CONTRACT_BUILDER = "ContractBuilder"

    private fun contractBuilder(name: String): CallableId = id(CONTRACT_PACKAGE, CONTRACT_BUILDER, name)
    private fun invocationKind(name: String): CallableId = id(CONTRACT_PACKAGE, INVOCATION_KIND_ENUM.callableName.asString(), name)
    private fun simpleEffect(name: String): CallableId = id(CONTRACT_PACKAGE, SIMPLE_EFFECT.callableName.asString(), name)
    private fun id(name: String): CallableId = id(CONTRACT_PACKAGE, name)
    private fun id(packageName: String, name: String): CallableId = id(packageName, className = null, name)
    internal fun id(packageName: String, className: String?, name: String): CallableId {
        return CallableId(
            FqName(packageName),
            className?.let { FqName(it) },
            Name.identifier(name)
        )
    }

    private const val CONTRACT_PACKAGE = "kotlin.contracts"
}