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

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline.Companion.PARAMETER_USAGE_KEY
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline.Companion.TYPE_PARAMETER_USAGE_KEY
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Represents code to be inlined to replace usages of particular callable.
 * The expression should be preprocessed in the following way:
 * * Type arguments for all calls should be made explicit
 * * All external symbols to be imported should be either referenced via fully-qualified form or included into [fqNamesToImport]
 * * All usages of value parameters (of our callable) should be marked with [PARAMETER_USAGE_KEY] copyable user data (holds the name of the corresponding parameter)
 * * All usages of type parameters (of our callable) should be marked with [TYPE_PARAMETER_USAGE_KEY] copyable user data (holds the name of the corresponding type parameter)
 * Use [CodeToInlineBuilder.prepareCodeToInline].
 */
class CodeToInline(
    val mainExpression: KtExpression?,
    val statementsBefore: List<KtExpression>,
    val fqNamesToImport: Collection<FqName>
) {
    companion object {
        val PARAMETER_USAGE_KEY: Key<Name> = Key("PARAMETER_USAGE")
        val TYPE_PARAMETER_USAGE_KEY: Key<Name> = Key("TYPE_PARAMETER_USAGE")
    }
}
