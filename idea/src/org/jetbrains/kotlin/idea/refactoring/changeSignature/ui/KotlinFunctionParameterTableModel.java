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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase;
import com.intellij.refactoring.ui.StringTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class KotlinFunctionParameterTableModel extends KotlinCallableParameterTableModel {
    public KotlinFunctionParameterTableModel(KotlinMethodDescriptor methodDescriptor, PsiElement context) {
        super(methodDescriptor,
              context,
              new NameColumn(context.getProject()),
              new TypeColumn(context.getProject(), KotlinFileType.INSTANCE),
              new DefaultValueColumn<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>>(context.getProject(),
                                                                                                            KotlinFileType.INSTANCE),
              new ReceiverColumn(context.getProject(), methodDescriptor));
    }

    @Override
    public void removeRow(int idx) {
        if (getRowValue(idx).parameter == getReceiver()) {
            setReceiver(null);
        }
        super.removeRow(idx);
    }

    @Override
    @Nullable
    public KotlinParameterInfo getReceiver() {
        return ((ReceiverColumn)getColumnInfos()[getColumnCount() - 1]).receiver;
    }

    public void setReceiver(@Nullable KotlinParameterInfo receiver) {
        ((ReceiverColumn)getColumnInfos()[getColumnCount() - 1]).receiver = receiver;
    }

    public static boolean isReceiverColumn(ColumnInfo column) {
        return column instanceof ReceiverColumn;
    }

    protected static class ReceiverColumn<TableItem extends ParameterTableModelItemBase<KotlinParameterInfo>>
            extends ColumnInfoBase<KotlinParameterInfo, TableItem, Boolean> {
        private final Project project;
        @Nullable
        private KotlinParameterInfo receiver;

        public ReceiverColumn(Project project, @Nullable KotlinMethodDescriptor methodDescriptor) {
            super("Receiver:");
            this.project = project;
            this.receiver = methodDescriptor != null ? methodDescriptor.getReceiver() : null;
        }

        @Override
        public Boolean valueOf(TableItem item) {
            return item.parameter == receiver;
        }

        @Override
        public void setValue(TableItem item, Boolean value) {
            if (value == null) return;
            receiver = value ? item.parameter : null;
        }

        @Override
        public boolean isCellEditable(TableItem pParameterTableModelItemBase) {
            return true;
        }

        @Override
        public TableCellRenderer doCreateRenderer(TableItem item) {
            return new BooleanTableCellRenderer();
        }

        @Override
        public TableCellEditor doCreateEditor(TableItem o) {
            return new StringTableCellEditor(project);
        }
    }
}
