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

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.config.LanguageFeatureSettings
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.KotlinType

interface CallChecker {
    // TODO: Think about encapsulating these parameters into specific class like CheckerParameters when you're about to add another one
    fun check(
            resolvedCall: ResolvedCall<*>,
            context: BasicCallResolutionContext,
            languageFeatureSettings: LanguageFeatureSettings
    )
}

interface SimpleCallChecker : CallChecker {
    override fun check(
            resolvedCall: ResolvedCall<*>,
            context: BasicCallResolutionContext,
            languageFeatureSettings: LanguageFeatureSettings
    ) = check(resolvedCall, context)

    fun check(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext)
}

// Use this utility to avoid premature computation of deferred return type of a resolved callable descriptor.
// Computing it in CallChecker#check is not feasible since it would trigger "type checking has run into a recursive problem" errors.
// Receiver parameter is present to emphasize that this function should ideally be only used from call checkers.
@Suppress("unused")
fun CallChecker.isComputingDeferredType(type: KotlinType) =
        type is DeferredType && type.isComputing
