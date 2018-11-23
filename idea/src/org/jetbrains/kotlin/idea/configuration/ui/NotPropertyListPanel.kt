/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration.ui

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.NonEmptyInputValidator
import com.intellij.ui.AddEditRemovePanel
import org.jetbrains.kotlin.name.FqNameUnsafe

class NotPropertyListPanel(data: MutableList<FqNameUnsafe>) : AddEditRemovePanel<FqNameUnsafe>(MyTableModel(), data) {

    var modified = false

    override fun removeItem(fqName: FqNameUnsafe): Boolean {
        modified = true
        return true
    }

    override fun editItem(fqName: FqNameUnsafe): FqNameUnsafe? {
        val result = Messages.showInputDialog(this, "Enter fully-qualified method name:",
                                              "Edit exclusion",
                                              Messages.getQuestionIcon(),
                                              fqName.asString(),
                                              NonEmptyInputValidator()
        ) ?: return null

        val created = FqNameUnsafe(result)

        if (created in data)
            return null

        modified = true
        return created
    }

    override fun addItem(): FqNameUnsafe? {
        val result = Messages.showInputDialog(this, "Enter fully-qualified method name:",
                                              "Add exclusion",
                                              Messages.getQuestionIcon(),
                                              "",
                                              NonEmptyInputValidator()
        ) ?: return null

        val created = FqNameUnsafe(result)

        if (created in data)
            return null

        modified = true
        return created
    }

    class MyTableModel : AddEditRemovePanel.TableModel<FqNameUnsafe>() {
        override fun getField(o: FqNameUnsafe, columnIndex: Int) = o.asString()
        override fun getColumnName(columnIndex: Int) = "Method"
        override fun getColumnCount() = 1
    }
}

