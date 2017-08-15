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

package org.jetbrains.kotlin.resolve.calls.model


sealed class ResolvedCallArgument {
    abstract val arguments: List<KotlinCallArgument>

    object DefaultArgument : ResolvedCallArgument() {
        override val arguments: List<KotlinCallArgument>
            get() = emptyList()

    }

    class SimpleArgument(val callArgument: KotlinCallArgument): ResolvedCallArgument() {
        override val arguments: List<KotlinCallArgument>
            get() = listOf(callArgument)

    }

    class VarargArgument(override val arguments: List<KotlinCallArgument>): ResolvedCallArgument()
}