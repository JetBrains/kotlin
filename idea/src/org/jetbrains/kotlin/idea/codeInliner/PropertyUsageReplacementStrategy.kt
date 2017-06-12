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

package org.jetbrains.kotlin.idea.codeInliner

import org.jetbrains.kotlin.idea.references.ReferenceAccess
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class PropertyUsageReplacementStrategy(readReplacement: CodeToInline?, writeReplacement: CodeToInline?) : UsageReplacementStrategy {
    private val readReplacementStrategy = readReplacement?.let { CallableUsageReplacementStrategy(it) }
    private val writeReplacementStrategy = writeReplacement?.let { CallableUsageReplacementStrategy(it) }

    override fun createReplacer(usage: KtSimpleNameExpression): (() -> KtElement?)? {
        val access = usage.readWriteAccess(useResolveForReadWrite = true)
        return when (access) {
            ReferenceAccess.READ -> readReplacementStrategy?.createReplacer(usage)
            ReferenceAccess.WRITE -> writeReplacementStrategy?.createReplacer(usage)
            ReferenceAccess.READ_WRITE -> null
        }
    }
}