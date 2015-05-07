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

package org.jetbrains.kotlin.psi

import com.intellij.psi.impl.source.tree.LeafPsiElement

public trait ValueArgument {
    IfNotParsed
    public fun getArgumentExpression(): JetExpression?

    public fun getArgumentName(): JetValueArgumentName?

    public fun isNamed(): Boolean

    public fun asElement(): JetElement

    /* The '*' in something like foo(*arr) i.e. pass an array as a number of vararg arguments */
    public fun getSpreadElement(): LeafPsiElement?

    /* The argument is placed externally to call element, e.g. in 'when' condition with subject: 'when (a) { in c -> }' */
    public fun isExternal(): Boolean
}

public trait FunctionLiteralArgument : ValueArgument {
    public fun getFunctionLiteral(): JetFunctionLiteralExpression

    override fun getArgumentExpression(): JetExpression
}
