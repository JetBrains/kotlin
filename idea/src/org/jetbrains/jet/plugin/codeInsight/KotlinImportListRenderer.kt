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

package org.jetbrains.jet.plugin.codeInsight

import com.intellij.ide.util.FQNameCellRenderer
import javax.swing.JList
import java.awt.Component

class KotlinImportListRenderer: FQNameCellRenderer() {
    override fun getListCellRendererComponent(list: JList?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component? {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        setIcon(null)
        return component
    }
}