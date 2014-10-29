/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.psi.JetCodeFragment

import java.util.Collections
import org.jetbrains.jet.utils.addToStdlib.singletonOrEmptyList

public abstract class JetSingleIntentionActionFactory : JetIntentionActionsFactory {
    public abstract fun createAction(diagnostic: Diagnostic): IntentionAction?

    override fun createActions(diagnostic: Diagnostic): List<IntentionAction> {
        if (diagnostic.getPsiElement().getContainingFile() is JetCodeFragment && !isApplicableForCodeFragment()) {
            return Collections.emptyList()
        }
        return createAction(diagnostic).singletonOrEmptyList()
    }

    public fun isApplicableForCodeFragment(): Boolean = false
}
