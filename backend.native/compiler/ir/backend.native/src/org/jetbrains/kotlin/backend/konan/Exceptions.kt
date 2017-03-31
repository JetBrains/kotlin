/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan

/**
 * Represents a compilation error caused by mistakes in an input file, e.g. undefined reference.
 */
class KonanCompilationException(message: String = "", cause: Throwable? = null) : Exception(message, cause) {}

/**
 * Internal compiler error: could not deserialize IR for inline function body.
 */
class KonanIrDeserializationException(message: String = "", cause: Throwable? = null) : Exception(message, cause)

