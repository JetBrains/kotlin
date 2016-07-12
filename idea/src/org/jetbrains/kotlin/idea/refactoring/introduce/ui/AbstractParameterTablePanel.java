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

package org.jetbrains.kotlin.idea.refactoring.introduce.ui;

import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.EditableModel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester;
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.Parameter;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public abstract class AbstractParameterTablePanel<Param, UIParam extends AbstractParameterTablePanel.AbstractParameterInfo<Param>> extends JPanel {
    public static abstract class AbstractParameterInfo<Param> {
        private final Param originalParameter;
        private boolean enabled = true;
        private String name;

        public AbstractParameterInfo(Param originalParameter) {
            this.originalParameter = originalParameter;
        }

        public Param getOriginalParameter() {
            return originalParameter;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public abstract Param toParameter();
    }

    protected List<UIParam> parameterInfos;

    private JBTable myTable;
    private TableModelBase myTableModel;

    public AbstractParameterTablePanel() {
        super(new BorderLayout());
    }

    protected TableModelBase createTableModel() {
        return new TableModelBase();
    }

    protected void createAdditionalColumns() {

    }

    public void init() {
        myTableModel = createTableModel();
        myTable = new JBTable(myTableModel);
        DefaultCellEditor defaultEditor = (DefaultCellEditor) myTable.getDefaultEditor(Object.class);
        defaultEditor.setClickCountToStart(1);

        myTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myTable.setCellSelectionEnabled(true);

        TableColumn checkBoxColumn = myTable.getColumnModel().getColumn(TableModelBase.CHECKMARK_COLUMN);
        TableUtil.setupCheckboxColumn(checkBoxColumn);
        checkBoxColumn.setHeaderValue("");
        checkBoxColumn.setCellRenderer(
                new BooleanTableCellRenderer() {
                    @NotNull
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
                    ) {
                        Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        rendererComponent.setEnabled(AbstractParameterTablePanel.this.isEnabled());
                        return rendererComponent;
                    }
                }
        );

        myTable.getColumnModel().getColumn(TableModelBase.PARAMETER_NAME_COLUMN).setHeaderValue("Name");

        createAdditionalColumns();

        myTable.setPreferredScrollableViewportSize(new Dimension(250, myTable.getRowHeight() * 5));
        myTable.setShowGrid(false);
        myTable.setIntercellSpacing(new Dimension(0, 0));

        @NonNls InputMap inputMap = myTable.getInputMap();
        @NonNls ActionMap actionMap = myTable.getActionMap();

        // SPACE: toggle enable/disable
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "enable_disable");
        actionMap.put("enable_disable", new AbstractAction() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                if (myTable.isEditing()) return;
                int[] rows = myTable.getSelectedRows();
                if (rows.length > 0) {
                    boolean valueToBeSet = false;
                    for (int row : rows) {
                        if (!parameterInfos.get(row).isEnabled()) {
                            valueToBeSet = true;
                            break;
                        }
                    }
                    for (int row : rows) {
                        parameterInfos.get(row).setEnabled(valueToBeSet);
                    }
                    myTableModel.fireTableRowsUpdated(rows[0], rows[rows.length - 1]);
                    TableUtil.selectRows(myTable, rows);
                }
            }
        });

        // make ENTER work when the table has focus
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "invoke_impl");
        actionMap.put("invoke_impl", new AbstractAction() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                TableCellEditor editor = myTable.getCellEditor();
                if (editor != null) {
                    editor.stopCellEditing();
                }
                else {
                    onEnterAction();
                }
            }
        });

        // make ESCAPE work when the table has focus
        actionMap.put("doCancel", new AbstractAction() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                TableCellEditor editor = myTable.getCellEditor();
                if (editor != null) {
                    editor.stopCellEditing();
                }
                else {
                    onCancelAction();
                }
            }
        });

        JPanel listPanel = ToolbarDecorator.createDecorator(myTable).disableAddAction().disableRemoveAction().createPanel();
        add(listPanel, BorderLayout.CENTER);
    }

    protected void updateSignature() {

    }

    protected void onEnterAction() {

    }

    protected void onCancelAction() {

    }

    protected class TableModelBase extends AbstractTableModel implements EditableModel {
        public static final int CHECKMARK_COLUMN = 0;
        public static final int PARAMETER_NAME_COLUMN = 1;

        @Override
        public void addRow() {
            throw new IllegalAccessError("Not implemented");
        }

        @Override
        public void removeRow(int index) {
            throw new IllegalAccessError("Not implemented");
        }

        @Override
        public void exchangeRows(int oldIndex, int newIndex) {
            if (oldIndex < 0 || newIndex < 0) return;
            if (oldIndex >= parameterInfos.size() || newIndex >= parameterInfos.size()) return;

            UIParam old = parameterInfos.get(oldIndex);
            parameterInfos.set(oldIndex, parameterInfos.get(newIndex));
            parameterInfos.set(newIndex, old);

            fireTableRowsUpdated(Math.min(oldIndex, newIndex), Math.max(oldIndex, newIndex));
            updateSignature();
        }

        @Override
        public boolean canExchangeRows(int oldIndex, int newIndex) {
            if (oldIndex < 0 || newIndex < 0) return false;
            if (oldIndex >= parameterInfos.size() || newIndex >= parameterInfos.size()) return false;
            return true;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return parameterInfos.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case CHECKMARK_COLUMN:
                    return parameterInfos.get(rowIndex).isEnabled();
                case PARAMETER_NAME_COLUMN:
                    return parameterInfos.get(rowIndex).getName();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            AbstractParameterInfo info = parameterInfos.get(rowIndex);
            switch (columnIndex) {
                case CHECKMARK_COLUMN: {
                    info.setEnabled((Boolean) aValue);
                    fireTableRowsUpdated(rowIndex, rowIndex);
                    myTable.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
                    updateSignature();
                    break;
                }
                case PARAMETER_NAME_COLUMN: {
                    String name = (String) aValue;
                    if (KotlinNameSuggester.INSTANCE.isIdentifier(name)) {
                        info.setName(name);
                    }
                    updateSignature();
                    break;
                }
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            AbstractParameterInfo info = parameterInfos.get(rowIndex);
            switch (columnIndex) {
                case CHECKMARK_COLUMN:
                    return isEnabled();
                case PARAMETER_NAME_COLUMN:
                    return isEnabled() && info.isEnabled();
                default:
                    return false;
            }
        }

        @NotNull
        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex == CHECKMARK_COLUMN) {
                return Boolean.class;
            }
            return super.getColumnClass(columnIndex);
        }
    }

    @NotNull
    public JBTable getTable() {
        return myTable;
    }

    @NotNull
    public TableModelBase getTableModel() {
        return myTableModel;
    }
}
