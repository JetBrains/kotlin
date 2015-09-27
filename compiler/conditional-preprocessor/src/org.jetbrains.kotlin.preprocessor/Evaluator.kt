/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.preprocessor

interface Evaluator : (List<Conditional>) -> Boolean

abstract class PlatformEvaluator : Evaluator {
    final override fun invoke(conditions: List<Conditional>): Boolean = evaluate(conditions.filterIsInstance())

    open fun evaluate(conditions: List<Conditional.PlatformVersion>): Boolean
            = conditions.isEmpty() || conditions.any { match(it) }

    abstract fun match(platformCondition: Conditional.PlatformVersion): Boolean
}

data class JvmPlatformEvaluator(val version: Int): PlatformEvaluator() {
    override fun match(platformCondition: Conditional.PlatformVersion)
            = platformCondition is Conditional.JvmVersion && version in platformCondition.versionRange
    override fun toString() = "platform: JVM$version"
}

data class JsPlatformEvaluator(val ecmaScriptVersion: Int = 5): PlatformEvaluator() {
    override fun match(platformCondition: Conditional.PlatformVersion)
            = platformCondition is Conditional.JsVersion
    override fun toString() = "platform: JS"
}