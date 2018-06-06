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

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.filters.ConsoleFilterProviderEx
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.search.GlobalSearchScope

class KotlinConsoleFilterProvider : ConsoleFilterProviderEx {
    override fun getDefaultFilters(project: Project): Array<Filter> {
        return getDefaultFilters(project, GlobalSearchScope.allScope(project))
    }

    override fun getDefaultFilters(project: Project, scope: GlobalSearchScope): Array<Filter> {
        return arrayOf(KotlinConsoleFilter(project, scope))
    }
}

class KotlinConsoleFilter(val project: Project, val scope: GlobalSearchScope) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val messageSuffix = pattern.find(line) ?: return null
        val pathStart = line.indexOf(":\\").takeIf { it >= 0 }?.minus(1)
                        ?: line.indexOf("/").takeIf { it >= 0 }
                        ?: return null
        val path = line.substring(pathStart, messageSuffix.groups[1]!!.range.last + 1)
        val lineNumber = Integer.parseInt(messageSuffix.groupValues[2]) - 1
        val column = Integer.parseInt(messageSuffix.groupValues[3]) - 1
        val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)

        val offset = entireLength - line.length
        return Filter.Result(offset + pathStart, offset + messageSuffix.range.last, LocalFileHyperlinkInfo(path, lineNumber, column), attrs)
    }

    companion object {
        private val pattern = Regex("""(\.kts?): \((\d+), (\d+)\):""")
    }
}

class LocalFileHyperlinkInfo(val path: String, val line: Int, val column: Int) : HyperlinkInfo {
    override fun navigate(project: Project) {
        val f = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path))
        if (f != null) {
            OpenFileDescriptor(project, f, line, column).navigate(true)
        }
    }
}
