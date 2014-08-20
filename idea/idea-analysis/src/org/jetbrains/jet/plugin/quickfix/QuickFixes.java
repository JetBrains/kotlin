/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.codeInsight.intention.IntentionAction;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;

import java.util.Collection;

public class QuickFixes {

    static final Multimap<DiagnosticFactory<?>, JetIntentionActionsFactory> factories = HashMultimap.create();
    static final Multimap<DiagnosticFactory<?>, IntentionAction> actions = HashMultimap.create();

    public static Collection<JetIntentionActionsFactory> getActionsFactories(DiagnosticFactory<?> diagnosticFactory) {
        return factories.get(diagnosticFactory);
    }

    public static Collection<IntentionAction> getActions(DiagnosticFactory<?> diagnosticFactory) {
        return actions.get(diagnosticFactory);
    }

    private QuickFixes() {}
}
