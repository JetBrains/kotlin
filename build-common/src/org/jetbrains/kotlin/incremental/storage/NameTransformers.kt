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

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

internal interface NameTransformer<Name> {
    fun asString(name: Name): String
    fun asName(string: String): Name
    fun asFqName(string: String): FqName
}

internal object FqNameTransformer : NameTransformer<FqName> {
    override fun asString(name: FqName): String =
        name.asString()

    override fun asName(string: String): FqName =
        FqName(string)

    override fun asFqName(string: String): FqName =
        asName(string)
}

internal object JvmClassNameTransformer : NameTransformer<JvmClassName> {
    override fun asString(name: JvmClassName): String =
        name.internalName

    override fun asName(string: String): JvmClassName =
        JvmClassName.byInternalName(string)

    override fun asFqName(string: String): FqName =
        asName(string).fqNameForClassNameWithoutDollars
}