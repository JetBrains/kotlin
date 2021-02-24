/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.NonEmptyInputValidator
import com.intellij.ui.AddEditRemovePanel
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.name.FqNameUnsafe

class NotPropertyListPanel(data: MutableList<FqNameUnsafe>) : AddEditRemovePanel<FqNameUnsafe>(MyTableModel(), data) {

    var modified = false

    override fun removeItem(fqName: FqNameUnsafe): Boolean {
        modified = true
        return true
    }

    override fun editItem(fqName: FqNameUnsafe): FqNameUnsafe? {
        val result = Messages.showInputDialog(
            this, KotlinBundle.message("configuration.message.enter.fully.qualified.method.name"),
            KotlinBundle.message("configuration.title.edit.exclusion"),
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
        val result = Messages.showInputDialog(
            this, KotlinBundle.message("configuration.message.enter.fully.qualified.method.name"),
            KotlinBundle.message("configuration.text.add.exclusion"),
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
        override fun getColumnName(columnIndex: Int) = KotlinBundle.message("configuration.name.method")
        override fun getColumnCount() = 1
    }
}

