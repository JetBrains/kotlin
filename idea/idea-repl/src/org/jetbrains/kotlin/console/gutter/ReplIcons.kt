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
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

data class IconWithTooltip(val icon: Icon, val tooltip: String?)

object ReplIcons {
    val BUILD_WARNING_INDICATOR: IconWithTooltip = IconWithTooltip(AllIcons.Ide.Warning_notifications, null)
    val HISTORY_INDICATOR: IconWithTooltip = IconWithTooltip(AllIcons.General.MessageHistory, "History of executed commands")
    val EDITOR_INDICATOR: IconWithTooltip = IconWithTooltip(KotlinIcons.LAUNCH, "Write your commands here")
    val EDITOR_READLINE_INDICATOR: IconWithTooltip = IconWithTooltip(AllIcons.General.Balloon, "Waiting for input...")
    val COMMAND_MARKER: IconWithTooltip = IconWithTooltip(AllIcons.General.Run, "Executed command")
    val READLINE_MARKER: IconWithTooltip = IconWithTooltip(AllIcons.Icons.Ide.SpeedSearchPrompt, "Input line")

    // command result icons
    val SYSTEM_HELP: IconWithTooltip = IconWithTooltip(AllIcons.Actions.Menu_help, "System help")
    val RESULT: IconWithTooltip = IconWithTooltip(AllIcons.Vcs.Equal, "Result")
    val COMPILER_ERROR: Icon = AllIcons.General.Error
    val RUNTIME_EXCEPTION: IconWithTooltip = IconWithTooltip(AllIcons.General.BalloonWarning, "Runtime exception")
}