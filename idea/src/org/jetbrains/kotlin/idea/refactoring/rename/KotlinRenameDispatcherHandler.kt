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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameHandler
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class KotlinRenameDispatcherHandler : RenameHandler {
    companion object {
        val EP_NAME = ExtensionPointName<RenameHandler>("org.jetbrains.kotlin.renameHandler")

        private val handlers: Array<out RenameHandler> get() = Extensions.getExtensions(EP_NAME)
    }

    internal fun getRenameHandler(dataContext: DataContext): RenameHandler? {
        val availableHandlers = handlers.filterTo(LinkedHashSet()) { it.isRenaming(dataContext) }
        availableHandlers.singleOrNull()?.let { return it }
        availableHandlers.firstIsInstanceOrNull<KotlinMemberInplaceRenameHandler>()?.let { availableHandlers -= it }
        return availableHandlers.firstOrNull()
    }

    override fun isAvailableOnDataContext(dataContext: DataContext) = handlers.any { it.isAvailableOnDataContext(dataContext) }

    override fun isRenaming(dataContext: DataContext) = isAvailableOnDataContext(dataContext)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
        getRenameHandler(dataContext)?.invoke(project, editor, file, dataContext)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        getRenameHandler(dataContext)?.invoke(project, elements, dataContext)
    }
}