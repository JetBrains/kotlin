/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.VariableReference]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeValueParameterReference]
 */
public open class KtValueParameterReference(public val name: String)

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanVariableReference]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanValueParameterReference]
 */
public class KtBooleanValueParameterReference(name: String) : KtValueParameterReference(name)
