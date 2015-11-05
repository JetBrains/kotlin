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

package org.jetbrains.kotlin.idea.refactoring.safeDelete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.HelpID;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.usages.impl.UsagePreviewPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtil;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtPsiUtil;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
*  Mostly copied from com.intellij.refactoring.safeDelete.OverridingMethodsDialog
*  Revision: 14aa2e2
*  (replace PsiMethod formatting)
*/
class KotlinOverridingDialog extends DialogWrapper {
    private final List<UsageInfo> myOverridingMethods;
    private final String[] myMethodText;
    private final boolean[] myChecked;

    private static final int CHECK_COLUMN = 0;
    private JBTable myTable;
    private final UsagePreviewPanel myUsagePreviewPanel;

    public KotlinOverridingDialog(Project project, List<UsageInfo> overridingMethods) {
        super(project, true);
        myOverridingMethods = overridingMethods;
        myChecked = new boolean[myOverridingMethods.size()];
        for (int i = 0; i < myChecked.length; i++) {
            myChecked[i] = true;
        }

        myMethodText = new String[myOverridingMethods.size()];
        for (int i = 0; i < myMethodText.length; i++) {
            myMethodText[i] = formatElement(((KotlinSafeDeleteOverridingUsageInfo) myOverridingMethods.get(i)).getOverridingElement());
        }
        myUsagePreviewPanel = new UsagePreviewPanel(project, new UsageViewPresentation());
        setTitle(KotlinBundle.message("unused.overriding.methods.title"));
        init();
    }

    private static String formatElement(PsiElement element) {
        element = KtPsiUtil.ascendIfPropertyAccessor(element);
        if (element instanceof KtNamedFunction || element instanceof KtProperty) {
            BindingContext bindingContext = ResolutionUtils.analyze((KtElement) element, BodyResolveMode.FULL);

            DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            if (declarationDescriptor instanceof CallableMemberDescriptor) {
                DeclarationDescriptor containingDescriptor = declarationDescriptor.getContainingDeclaration();
                if (containingDescriptor instanceof ClassDescriptor) {
                    return KotlinBundle.message(
                            "x.in.y",
                            DescriptorRenderer.COMPACT.render(declarationDescriptor),
                            IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(containingDescriptor)
                    );
                }
            }
        }

        assert element instanceof PsiMethod
                : "Method accepts only kotlin functions/properties and java methods, but '" + element.getText() + "' was found";
        return KotlinRefactoringUtil.formatPsiMethod((PsiMethod) element, true, false);
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#org.jetbrains.kotlin.idea.refactoring.safeDelete.KotlinOverridingDialog";
    }

    @NotNull
    public List<UsageInfo> getSelected() {
        List<UsageInfo> result = new ArrayList<UsageInfo>();
        for (int i = 0; i < myChecked.length; i++) {
            if (myChecked[i]) {
                result.add(myOverridingMethods.get(i));
            }
        }
        return result;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[] {getOKAction(), getCancelAction()};
    }

    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(HelpID.SAFE_DELETE_OVERRIDING);
    }

    @Override
    protected JComponent createNorthPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(KotlinBundle.message("there.are.unused.methods.that.override.methods.you.delete")));
        panel.add(new JLabel(KotlinBundle.message("choose.the.ones.you.want.to.be.deleted")));
        return panel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myTable;
    }

    @Override
    protected void dispose() {
        Disposer.dispose(myUsagePreviewPanel);
        super.dispose();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        final MyTableModel tableModel = new MyTableModel();
        myTable = new JBTable(tableModel);
        myTable.setShowGrid(false);

        TableColumnModel columnModel = myTable.getColumnModel();
        int checkBoxWidth = new JCheckBox().getPreferredSize().width;
        columnModel.getColumn(CHECK_COLUMN).setCellRenderer(new BooleanTableCellRenderer());
        columnModel.getColumn(CHECK_COLUMN).setMaxWidth(checkBoxWidth);
        columnModel.getColumn(CHECK_COLUMN).setMinWidth(checkBoxWidth);


        // make SPACE check/uncheck selected rows
        InputMap inputMap = myTable.getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "enable_disable");
        ActionMap actionMap = myTable.getActionMap();
        actionMap.put("enable_disable", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (myTable.isEditing()) return;
                int[] rows = myTable.getSelectedRows();
                if (rows.length > 0) {
                    boolean valueToBeSet = false;
                    for (int row : rows) {
                        if (!myChecked[row]) {
                            valueToBeSet = true;
                            break;
                        }
                    }
                    for (int row : rows) {
                        myChecked[row] = valueToBeSet;
                    }

                    tableModel.updateData();
                }
            }
        });

        panel.setLayout(new BorderLayout());

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable);

        panel.add(scrollPane, BorderLayout.CENTER);
        ListSelectionListener selectionListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = myTable.getSelectionModel().getLeadSelectionIndex();
                if (index != -1) {
                    UsageInfo usageInfo = myOverridingMethods.get(index);
                    myUsagePreviewPanel.updateLayout(Collections.singletonList(usageInfo));
                }
                else {
                    myUsagePreviewPanel.updateLayout(null);
                }
            }
        };
        myTable.getSelectionModel().addListSelectionListener(selectionListener);

        final Splitter splitter = new Splitter(true, 0.3f);
        splitter.setFirstComponent(panel);
        splitter.setSecondComponent(myUsagePreviewPanel);
        myUsagePreviewPanel.updateLayout(null);

        Disposer.register(myDisposable, new Disposable() {
            @Override
            public void dispose() {
                splitter.dispose();
            }
        });

        if (tableModel.getRowCount() != 0) {
            myTable.getSelectionModel().addSelectionInterval(0, 0);
        }
        return splitter;
    }

    class MyTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return myChecked.length;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case CHECK_COLUMN:
                    return " ";
                default:
                    return KotlinBundle.message("method.column");
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case CHECK_COLUMN:
                    return Boolean.class;
                default:
                    return String.class;
            }
        }


        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == CHECK_COLUMN) {
                return Boolean.valueOf(myChecked[rowIndex]);
            }
            else {
                return myMethodText[rowIndex];
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == CHECK_COLUMN) {
                myChecked[rowIndex] = ((Boolean) aValue).booleanValue();
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == CHECK_COLUMN;
        }

        void updateData() {
            fireTableDataChanged();
        }
    }
}

