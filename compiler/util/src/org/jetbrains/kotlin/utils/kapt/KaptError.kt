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

package org.jetbrains.kotlin.kapt3.diagnostic

class KaptError : RuntimeException {
    val kind: Kind

    enum class Kind(val message: String) {
        EXCEPTION("Exception while annotation processing"),
        ERROR_RAISED("Error while annotation processing"),
    }

    constructor(kind: Kind) : super(kind.message) {
        this.kind = kind
    }

    constructor(kind: Kind, cause: Throwable) : super(kind.message, cause) {
        this.kind = kind
    }
}