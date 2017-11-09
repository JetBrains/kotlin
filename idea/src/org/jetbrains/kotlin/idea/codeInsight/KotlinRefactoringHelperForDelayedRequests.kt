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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.refactoring.RefactoringHelper
import com.intellij.usageView.UsageInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.codeInsight.shorten.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction

class KotlinRefactoringHelperForDelayedRequests : RefactoringHelper<Any> {
    override fun prepareOperation(usages: Array<out UsageInfo>?): Any? {
        if (usages != null && usages.isNotEmpty()) {
            val project = usages[0].project
            prepareDelayedRequests(project)
        }
        return null
    }

    override fun performOperation(project: Project, operationData: Any?) {
        runWriteAction { performDelayedRefactoringRequests(project) }
    }
}
