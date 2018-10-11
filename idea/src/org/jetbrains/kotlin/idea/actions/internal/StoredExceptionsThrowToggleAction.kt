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

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.utils.WrappedValues


class StoredExceptionsThrowToggleAction : ToggleAction("Internal: toggle throwing cached PCE", "Rethrow stored PCE as a new runtime exception", null) {
    override fun isSelected(e: AnActionEvent): Boolean {
        return WrappedValues.throwWrappedProcessCanceledException
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        WrappedValues.throwWrappedProcessCanceledException = state
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        e.presentation.isEnabled = ApplicationManager.getApplication().isInternal
    }
}
