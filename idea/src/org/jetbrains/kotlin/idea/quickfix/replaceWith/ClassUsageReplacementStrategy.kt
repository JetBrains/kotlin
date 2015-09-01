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

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetNameReferenceExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetUserType

class ClassUsageReplacementStrategy(
        private val replacement: JetUserType
) : UsageReplacementStrategy {

    override fun createReplacer(usage: JetSimpleNameExpression): (() -> JetElement)? {
        if (usage !is JetNameReferenceExpression) return null

        val parent = usage.parent
        when (parent) {
            is JetUserType -> {
                return {
                    val replaced = parent.replaced(replacement)
                    ShortenReferences.DEFAULT.process(replaced)
                } //TODO: type arguments and type arguments of outer class are lost
            }

            else -> return null //TODO
        }
    }
}