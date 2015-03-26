/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.changeSignature;

import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase;
import com.intellij.util.ui.ColumnInfo;
import org.jdesktop.swingx.autocomplete.ComboBoxCellEditor;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class JetConstructorParameterTableModel extends JetCallableParameterTableModel {
    public JetConstructorParameterTableModel(PsiElement context) {
        super(context,
              new ValVarColumn(),
              new NameColumn(context.getProject()),
              new TypeColumn(context.getProject(), JetFileType.INSTANCE),
              new DefaultValueColumn<JetParameterInfo, ParameterTableModelItemBase<JetParameterInfo>>(context.getProject(), JetFileType.INSTANCE));
    }

    public static boolean isValVarColumn(ColumnInfo column) {
        return column instanceof ValVarColumn;
    }

    protected static class ValVarColumn extends ColumnInfoBase<JetParameterInfo, ParameterTableModelItemBase<JetParameterInfo>, JetValVar> {
        public ValVarColumn() {
            super(JetRefactoringBundle.message("column.name.val.var"));
        }

        @Override
        public boolean isCellEditable(ParameterTableModelItemBase<JetParameterInfo> item) {
            return !item.isEllipsisType() && item.parameter.getIsNewParameter();
        }

        @Override
        public JetValVar valueOf(ParameterTableModelItemBase<JetParameterInfo> item) {
            return item.parameter.getValOrVar();
        }

        @Override
        public void setValue(ParameterTableModelItemBase<JetParameterInfo> item, JetValVar value) {
            item.parameter.setValOrVar(value);
        }

        @Override
        public TableCellRenderer doCreateRenderer(ParameterTableModelItemBase<JetParameterInfo> item) {
            return new ComboBoxTableRenderer<JetValVar>(JetValVar.values());
        }

        @Override
        public TableCellEditor doCreateEditor(ParameterTableModelItemBase<JetParameterInfo> item) {
            return new ComboBoxCellEditor(new JComboBox());
        }

        @Override
        public int getWidth(JTable table) {
            return table.getFontMetrics(table.getFont()).stringWidth(getName()) + 8;
        }
    }
}
