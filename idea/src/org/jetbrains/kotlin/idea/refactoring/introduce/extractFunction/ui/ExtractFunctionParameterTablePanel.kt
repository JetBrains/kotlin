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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui;

import com.intellij.ui.components.JBComboBoxLabel;
import com.intellij.ui.components.editors.JBComboBoxTableCellEditorComponent;
import com.intellij.ui.table.JBTable;
import com.intellij.util.Function;
import com.intellij.util.ui.AbstractTableCellEditor;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.Parameter;
import org.jetbrains.kotlin.idea.refactoring.introduce.ui.AbstractParameterTablePanel;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.types.KotlinType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ExtractFunctionParameterTablePanel extends AbstractParameterTablePanel<Parameter, ParameterInfo> {
    @Override
    protected TableModelBase createTableModel() {
        return new MyTableModel();
    }

    @Override
    protected void createAdditionalColumns() {
        JBTable table = getTable();

        TableColumn parameterTypeColumn = table.getColumnModel().getColumn(MyTableModel.PARAMETER_TYPE_COLUMN);
        parameterTypeColumn.setHeaderValue("Type");
        parameterTypeColumn.setCellRenderer(new DefaultTableCellRenderer() {
            private final JBComboBoxLabel myLabel = new JBComboBoxLabel();

            @Override
            @NotNull
            public Component getTableCellRendererComponent(
                    @NotNull JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
            ) {
                myLabel.setText(IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType((KotlinType) value));
                myLabel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                myLabel.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                if (isSelected) {
                    myLabel.setSelectionIcon();
                }
                else {
                    myLabel.setRegularIcon();
                }
                return myLabel;
            }
        });
        parameterTypeColumn.setCellEditor(new AbstractTableCellEditor() {
            final JBComboBoxTableCellEditorComponent myEditorComponent = new JBComboBoxTableCellEditorComponent();

            @Override
            @Nullable
            public Object getCellEditorValue() {
                return myEditorComponent.getEditorValue();
            }

            @Override
            public Component getTableCellEditorComponent(
                    JTable table, Object value, boolean isSelected, int row, int column
            ) {
                ParameterInfo info = parameterInfos.get(row);

                myEditorComponent.setCell(table, row, column);
                myEditorComponent.setOptions(info.getOriginalParameter().getParameterTypeCandidates(false).toArray());
                myEditorComponent.setDefaultValue(info.getType());
                myEditorComponent.setToString(new Function<Object, String>() {
                    @Override
                    public String fun(Object o) {
                        return IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType((KotlinType) o);
                    }
                });

                return myEditorComponent;
            }
        });
    }

    public void init(@Nullable Parameter receiver, @NotNull List<Parameter> parameters) {
        parameterInfos = CollectionsKt.mapTo(
                parameters,
                receiver != null
                ? CollectionsKt.arrayListOf(new ParameterInfo(receiver, true))
                : new ArrayList<ParameterInfo>(),
                new Function1<Parameter, ParameterInfo>() {
                    @Override
                    public ParameterInfo invoke(Parameter parameter) {
                        return new ParameterInfo(parameter, false);
                    }
                }
        );

        super.init();
    }

    private class MyTableModel extends TableModelBase {
        public static final int PARAMETER_TYPE_COLUMN = 2;

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == PARAMETER_TYPE_COLUMN) return parameterInfos.get(rowIndex).getType();
            return super.getValueAt(rowIndex, columnIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == PARAMETER_TYPE_COLUMN) {
                parameterInfos.get(rowIndex).setType((KotlinType) aValue);
                updateSignature();
                return;
            }

            super.setValueAt(aValue, rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            ParameterInfo info = parameterInfos.get(rowIndex);
            switch (columnIndex) {
                case PARAMETER_NAME_COLUMN:
                    return super.isCellEditable(rowIndex, columnIndex) && !info.isReceiver();
                case PARAMETER_TYPE_COLUMN:
                    return isEnabled() && info.isEnabled() && info.getOriginalParameter().getParameterTypeCandidates(false).size() > 1;
                default:
                    return super.isCellEditable(rowIndex, columnIndex);
            }
        }
    }

    @Nullable
    public ParameterInfo getReceiverInfo() {
        return CollectionsKt.singleOrNull(
                parameterInfos,
                new Function1<ParameterInfo, Boolean>() {
                    @Override
                    public Boolean invoke(ParameterInfo info) {
                        return info.isEnabled() && info.isReceiver();
                    }
                }
        );
    }

    @NotNull
    public List<ParameterInfo> getParameterInfos() {
        return CollectionsKt.filter(
                parameterInfos,
                new Function1<ParameterInfo, Boolean>() {
                    @Override
                    public Boolean invoke(ParameterInfo info) {
                        return info.isEnabled() && !info.isReceiver();
                    }
                }
        );
    }
}
