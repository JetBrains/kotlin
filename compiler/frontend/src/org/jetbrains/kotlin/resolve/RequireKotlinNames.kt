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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object RequireKotlinNames {
    val FQ_NAME = FqName("kotlin.internal.RequireKotlin")

    val VERSION = Name.identifier("version")
    val MESSAGE = Name.identifier("message")
    val LEVEL = Name.identifier("level")
    val VERSION_KIND = Name.identifier("versionKind")
    val ERROR_CODE = Name.identifier("errorCode")
}
