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

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.refactoring.ui.AbstractMemberSelectionPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SeparatorFactory
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import java.awt.BorderLayout

public class KotlinMemberSelectionPanel(title: String,
                                        memberInfo: List<KotlinMemberInfo>,
                                        abstractColumnHeader: String?
) : AbstractMemberSelectionPanel<JetNamedDeclaration, KotlinMemberInfo>() {
    private val table = createMemberSelectionTable(memberInfo, abstractColumnHeader)

    init {
        layout = BorderLayout()

        val scrollPane = ScrollPaneFactory.createScrollPane(table)
        add(SeparatorFactory.createSeparator(title, table), BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }

    protected fun createMemberSelectionTable(
            memberInfo: List<KotlinMemberInfo>,
            abstractColumnHeader: String?
    ): KotlinMemberSelectionTable {
        return KotlinMemberSelectionTable(memberInfo, null, abstractColumnHeader)
    }

    override fun getTable() = table
}