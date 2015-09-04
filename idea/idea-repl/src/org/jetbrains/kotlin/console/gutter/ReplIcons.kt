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

package org.jetbrains.kotlin.console.gutter

import com.intellij.icons.AllIcons
import org.jetbrains.kotlin.idea.JetIcons
import javax.swing.Icon

public object ReplIcons {
    public val BUILD_WARNING_INDICATOR: Icon = AllIcons.Ide.Warning_notifications
    public val HISTORY_INDICATOR: Icon = AllIcons.General.MessageHistory
    public val EDITOR_INDICATOR: Icon = JetIcons.LAUNCH
    public val EDITOR_READLINE_INDICATOR: Icon = AllIcons.General.Balloon
    public val COMMAND_MARKER: Icon = AllIcons.General.Run
    public val READLINE_MARKER: Icon = AllIcons.Icons.Ide.SpeedSearchPrompt

    // command result icons
    public val SYSTEM_HELP: Icon = AllIcons.Actions.Menu_help
    public val RESULT: Icon = AllIcons.Vcs.Equal
    public val COMPILER_ERROR: Icon = AllIcons.General.Error
    public val RUNTIME_EXCEPTION: Icon = AllIcons.General.BalloonWarning
}