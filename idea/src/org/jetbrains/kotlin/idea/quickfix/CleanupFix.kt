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

/**
 * Marker interface for quickfixes that can be used as part of the "Cleanup Code" action. The diagnostics
 * that produce these quickfixes need to be added to KotlinCleanupInspection.cleanupDiagnosticsFactories.
 */
interface CleanupFix : IntentionAction {
}
// TODO(yole): add isSafeToApply() method here to get rid of filtering by diagnostics factories in
// KotlinCleanupInspection
