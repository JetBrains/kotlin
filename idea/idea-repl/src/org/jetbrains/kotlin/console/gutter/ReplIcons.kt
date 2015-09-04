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

public data class IconPack(val icon: Icon, val tooltip: String)

public object ReplIcons {
    public val BUILD_WARNING_INDICATOR: IconPack = IconPack(AllIcons.Ide.Warning_notifications, "Some issues with module <make>")
    public val HISTORY_INDICATOR: IconPack = IconPack(AllIcons.General.MessageHistory, "History of executed commands")
    public val EDITOR_INDICATOR: IconPack = IconPack(JetIcons.LAUNCH, "Write your commands here")
    public val EDITOR_READLINE_INDICATOR: IconPack = IconPack(AllIcons.General.Balloon, "Waiting for input...")
    public val COMMAND_MARKER: IconPack = IconPack(AllIcons.General.Run, "Executed command")
    public val READLINE_MARKER: IconPack = IconPack(AllIcons.Icons.Ide.SpeedSearchPrompt, "Input line")

    // command result icons
    public val SYSTEM_HELP: IconPack = IconPack(AllIcons.Actions.Menu_help, "System help")
    public val RESULT: IconPack = IconPack(AllIcons.Vcs.Equal, "Result")
    public val COMPILER_ERROR: Icon = AllIcons.General.Error
    public val RUNTIME_EXCEPTION: IconPack = IconPack(AllIcons.General.BalloonWarning, "Runtime exception")
}