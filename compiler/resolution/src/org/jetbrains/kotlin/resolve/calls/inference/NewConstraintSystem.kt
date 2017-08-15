/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.components.KotlinCallCompleter
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic

interface NewConstraintSystem {
    val builtIns: KotlinBuiltIns
    val hasContradiction: Boolean
    val diagnostics: List<KotlinCallDiagnostic>

    fun getBuilder(): ConstraintSystemBuilder

    // after this method we shouldn't mutate system via ConstraintSystemBuilder
    fun asReadOnlyStorage(): ConstraintStorage
    fun asConstraintSystemCompleterContext(): KotlinConstraintSystemCompleter.Context
    fun asPostponedArgumentsAnalyzerContext(): PostponedArgumentsAnalyzer.Context
}