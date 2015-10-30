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

package org.jetbrains.kotlin.incremental.components

public interface LookupLocation {
}

public enum class NoLookupLocation : LookupLocation {
    @Deprecated("Use more suitable constant if possible")
    UNSORTED,
    FROM_IDE,
    FROM_BACKEND,
    FROM_TEST,
    FROM_BUILTINS,
    WHEN_CHECK_REDECLARATIONS,
    FOR_SCRIPT,
    FROM_REFLECTION,
    WHEN_RESOLVE_DECLARATION,
    WHEN_GET_DECLARATION_SCOPE,
    FOR_ALREADY_TRACKED,
    // TODO replace with real location (e.g. FROM_IDE) where it possible
    WHEN_GET_ALL_DESCRIPTORS,
    WHEN_TYPING,
    WHEN_GET_SUPER_MEMBERS,
    FOR_NON_TRACKED_SCOPE,
    FROM_SYNTHETIC_SCOPE
}
