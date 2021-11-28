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

import org.jetbrains.kotlin.types.TypeCheckerState

@RequiresOptIn
annotation class ClassicTypeCheckerStateInternals

/**
 * Class [ClassicTypeCheckerState] exists only to provide default arguments for
 *   cases when someone need to implement custom [TypeCheckerState] using default
 *   arguments. If you need to create only an instance of [TypeCheckerState] please
 *   use [createClassicTypeCheckerState] method.
 *
 *  Also please don't use [ClassicTypeCheckerState] explicitly (except for inheritance)
 */
@ClassicTypeCheckerStateInternals
open class ClassicTypeCheckerState(
    isErrorTypeEqualsToAnything: Boolean,
    isStubTypeEqualsToAnything: Boolean = true,
    typeSystemContext: ClassicTypeSystemContext = SimpleClassicTypeSystemContext,
    kotlinTypePreparator: KotlinTypePreparator = KotlinTypePreparator.Default,
    kotlinTypeRefiner: KotlinTypeRefiner = KotlinTypeRefiner.Default
) : TypeCheckerState(
    isErrorTypeEqualsToAnything,
    isStubTypeEqualsToAnything,
    allowedTypeVariable = true,
    typeSystemContext,
    kotlinTypePreparator,
    kotlinTypeRefiner
)

fun createClassicTypeCheckerState(
    isErrorTypeEqualsToAnything: Boolean,
    isStubTypeEqualsToAnything: Boolean = true,
    typeSystemContext: ClassicTypeSystemContext = SimpleClassicTypeSystemContext,
    kotlinTypePreparator: KotlinTypePreparator = KotlinTypePreparator.Default,
    kotlinTypeRefiner: KotlinTypeRefiner = KotlinTypeRefiner.Default
): TypeCheckerState {
    return TypeCheckerState(
        isErrorTypeEqualsToAnything,
        isStubTypeEqualsToAnything,
        allowedTypeVariable = true,
        typeSystemContext,
        kotlinTypePreparator,
        kotlinTypeRefiner
    )
}
