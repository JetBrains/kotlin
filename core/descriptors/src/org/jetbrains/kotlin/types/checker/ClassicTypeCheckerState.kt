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

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

open class ClassicTypeCheckerState(
    val errorTypeEqualsToAnything: Boolean,
    val stubTypeEqualsToAnything: Boolean = true,
    override val kotlinTypeRefiner: KotlinTypeRefiner = KotlinTypeRefiner.Default,
    override val kotlinTypePreparator: KotlinTypePreparator = KotlinTypePreparator.Default,
    override val typeSystemContext: ClassicTypeSystemContext = SimpleClassicTypeSystemContext
) : TypeCheckerState() {
    override val isErrorTypeEqualsToAnything: Boolean
        get() = errorTypeEqualsToAnything

    override val isStubTypeEqualsToAnything: Boolean
        get() = stubTypeEqualsToAnything

    override val allowedTypeVariable: Boolean
        get() = true
}
