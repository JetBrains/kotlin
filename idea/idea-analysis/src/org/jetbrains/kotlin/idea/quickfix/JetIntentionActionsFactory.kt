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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.JetCodeFragment
import java.util.Collections

// TODO: Replace with trait when all subclasses are translated to Kotlin
public abstract class JetIntentionActionsFactory {
    protected open fun isApplicableForCodeFragment(): Boolean = false
    protected abstract fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>?

    public fun createActions(diagnostic: Diagnostic): List<IntentionAction> {
        if (diagnostic.getPsiElement().getContainingFile() is JetCodeFragment && !isApplicableForCodeFragment()) {
            return emptyList()
        }
        return doCreateActions(diagnostic) ?: emptyList()
    }
}
