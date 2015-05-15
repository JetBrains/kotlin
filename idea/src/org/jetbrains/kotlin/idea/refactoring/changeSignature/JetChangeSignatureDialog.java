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

package org.jetbrains.kotlin.idea.refactoring.changeSignature;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.changeSignature.CallerChooserBase;
import com.intellij.refactoring.changeSignature.ChangeSignatureDialogBase;
import com.intellij.refactoring.changeSignature.MethodDescriptor;
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase;
import com.intellij.refactoring.ui.ComboBoxVisibilityPanel;
import com.intellij.refactoring.ui.VisibilityPanelBase;
import com.intellij.ui.DottedBorder;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.table.JBTableRow;
import com.intellij.util.ui.table.JBTableRowEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.Visibilities;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetExpressionCodeFragment;
import org.jetbrains.kotlin.psi.JetTypeCodeFragment;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetMethodDescriptor.Kind;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class JetChangeSignatureDialog extends ChangeSignatureDialogBase<
        JetParameterInfo,
        PsiElement,
        Visibility,
        JetMethodDescriptor,
        ParameterTableModelItemBase<JetParameterInfo>,
        JetCallableParameterTableModel
        >
{
    private final String commandName;

    public JetChangeSignatureDialog(Project project, @NotNull JetMethodDescriptor methodDescriptor, PsiElement context, String commandName) {
        super(project, methodDescriptor, false, context);
        this.commandName = commandName;
    }

    @Override
    protected LanguageFileType getFileType() {
        return JetFileType.INSTANCE;
    }

    @Override
    protected JetCallableParameterTableModel createParametersInfoModel(JetMethodDescriptor descriptor) {
        return createParametersInfoModel(descriptor, myDefaultValueContext);
    }

    @NotNull
    private static JetCallableParameterTableModel createParametersInfoModel(JetMethodDescriptor descriptor, PsiElement defaultValueContext) {
        switch (descriptor.getKind()) {
            case FUNCTION:
                return new JetFunctionParameterTableModel(descriptor, defaultValueContext);
            case PRIMARY_CONSTRUCTOR:
                return new JetPrimaryConstructorParameterTableModel(descriptor, defaultValueContext);
            case SECONDARY_CONSTRUCTOR:
                return new JetSecondaryConstructorParameterTableModel(descriptor, defaultValueContext);
        }
        throw new AssertionError("Invalid kind: " + descriptor.getKind());
    }

    @Override
    protected PsiCodeFragment createReturnTypeCodeFragment() {
        return createReturnTypeCodeFragment(myProject, myMethod);
    }

    @NotNull
    private static PsiCodeFragment createReturnTypeCodeFragment(@NotNull Project project, @NotNull JetMethodDescriptor method) {
        return JetPsiFactory(project).createTypeCodeFragment(
                ChangeSignaturePackage.renderOriginalReturnType(method), method.getBaseDeclaration()
        );
    }

    @Nullable
    public JetType getReturnType() {
        return getType((JetTypeCodeFragment) myReturnTypeCodeFragment);
    }

    private static JetType getType(JetTypeCodeFragment typeCodeFragment) {
        return typeCodeFragment != null ? typeCodeFragment.getType() : null;
    }

    @Override
    protected JComponent getRowPresentation(ParameterTableModelItemBase<JetParameterInfo> item, boolean selected, boolean focused) {
        JPanel panel = new JPanel(new BorderLayout());
        String valOrVar = "";

        if (myMethod.getKind() == Kind.PRIMARY_CONSTRUCTOR) {
            switch (item.parameter.getValOrVar()) {
                case None:
                    valOrVar = "    ";
                    break;
                case Val:
                    valOrVar = "val ";
                    break;
                case Var:
                    valOrVar = "var ";
                    break;
            }
        }

        String parameterName = getPresentationName(item);
        String typeText = item.typeCodeFragment.getText();
        String defaultValue = item.defaultValueCodeFragment.getText();
        String separator = StringUtil.repeatSymbol(' ', getParamNamesMaxLength() - parameterName.length() + 1);
        String text = valOrVar + parameterName + ":" + separator + typeText;

        if (StringUtil.isNotEmpty(defaultValue))
            text += " // default value = " + defaultValue;

        EditorTextField field = new EditorTextField(" " + text, getProject(), getFileType()) {
            @Override
            protected boolean shouldHaveBorder() {
                return false;
            }
        };

        Font font = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
        font = new Font(font.getFontName(), font.getStyle(), 12);
        field.setFont(font);

        if (selected && focused) {
            panel.setBackground(UIUtil.getTableSelectionBackground());
            field.setAsRendererWithSelection(UIUtil.getTableSelectionBackground(), UIUtil.getTableSelectionForeground());
        } else {
            panel.setBackground(UIUtil.getTableBackground());
            if (selected && !focused) {
                panel.setBorder(new DottedBorder(UIUtil.getTableForeground()));
            }
        }
        panel.add(field, BorderLayout.WEST);
        return panel;
    }

    private String getPresentationName(ParameterTableModelItemBase<JetParameterInfo> item) {
        JetParameterInfo parameter = item.parameter;
        if (parameter == null) return null;
        if (parameter == myParametersTableModel.getReceiver()) return "<receiver>";
        return parameter.getName();
    }

    private int getColumnTextMaxLength(Function<ParameterTableModelItemBase<JetParameterInfo>, String> nameFunction) {
        int len = 0;
        for (ParameterTableModelItemBase<JetParameterInfo> item : myParametersTableModel.getItems()) {
            String text = nameFunction.fun(item);
            len = Math.max(len, text == null ? 0 : text.length());
        }
        return len;
    }

    private int getParamNamesMaxLength() {
        return getColumnTextMaxLength(new Function<ParameterTableModelItemBase<JetParameterInfo>, String>() {
            @Override
            public String fun(ParameterTableModelItemBase<JetParameterInfo> item) {
                return getPresentationName(item);
            }
        });
    }

    private int getTypesMaxLength() {
        return getColumnTextMaxLength(new Function<ParameterTableModelItemBase<JetParameterInfo>, String>() {
            @Override
            public String fun(ParameterTableModelItemBase<JetParameterInfo> item) {
                return item.typeCodeFragment == null ? null : item.typeCodeFragment.getText();
            }
        });
    }

    private int getDefaultValuesMaxLength() {
        return getColumnTextMaxLength(new Function<ParameterTableModelItemBase<JetParameterInfo>, String>() {
            @Override
            public String fun(ParameterTableModelItemBase<JetParameterInfo> item) {
                return item.defaultValueCodeFragment == null ? null : item.defaultValueCodeFragment.getText();
            }
        });
    }

    @Override
    protected boolean isListTableViewSupported() {
        return true;
    }

    @Override
    protected boolean isEmptyRow(ParameterTableModelItemBase<JetParameterInfo> row) {
        if (!StringUtil.isEmpty(row.parameter.getName())) return false;
        if (!StringUtil.isEmpty(row.parameter.getTypeText())) return false;
        return true;
    }

    @Nullable
    @Override
    protected CallerChooserBase<PsiElement> createCallerChooser(String title, Tree treeToReuse, Consumer<Set<PsiElement>> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected JBTableRowEditor getTableEditor(final JTable t, final ParameterTableModelItemBase<JetParameterInfo> item) {
        return new JBTableRowEditor() {
            private final List<JComponent> components = new ArrayList<JComponent>();
            private final EditorTextField nameEditor = new EditorTextField(item.parameter.getName(), getProject(), getFileType());;

            private void updateNameEditor() {
                nameEditor.setEnabled(item.parameter != myParametersTableModel.getReceiver());
            }

            private boolean isDefaultColumnEnabled() {
                return item.parameter.getIsNewParameter() && item.parameter != myMethod.getReceiver();
            }

            @Override
            public void prepareEditor(JTable table, final int row) {
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                int column = 0;

                for (ColumnInfo columnInfo : myParametersTableModel.getColumnInfos()) {
                    JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 4, 2, true, false));
                    EditorTextField editor = null;
                    JComponent component;
                    final int columnFinal = column;

                    if (JetCallableParameterTableModel.isTypeColumn(columnInfo)) {
                        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(item.typeCodeFragment);
                        component = editor = new EditorTextField(document, getProject(), getFileType());
                    }
                    else if (JetCallableParameterTableModel.isNameColumn(columnInfo)) {
                        component = editor = nameEditor;
                        updateNameEditor();
                    }
                    else if (JetCallableParameterTableModel.isDefaultValueColumn(columnInfo) && isDefaultColumnEnabled()) {
                        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(item.defaultValueCodeFragment);
                        component = editor = new EditorTextField(document, getProject(), getFileType());
                    }
                    else if (JetPrimaryConstructorParameterTableModel.isValVarColumn(columnInfo)) {
                        JComboBox comboBox = new JComboBox(JetValVar.values());
                        comboBox.setSelectedItem(item.parameter.getValOrVar());
                        comboBox.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(@NotNull ItemEvent e) {
                                myParametersTableModel.setValueAtWithoutUpdate(e.getItem(), row, columnFinal);
                                updateSignature();
                            }
                        });
                        component = comboBox;
                    }
                    else if (JetFunctionParameterTableModel.isReceiverColumn(columnInfo)) {
                        JCheckBox checkBox = new JCheckBox();
                        checkBox.setSelected(myParametersTableModel.getReceiver() == item.parameter);
                        checkBox.addItemListener(
                                new ItemListener() {
                                    @Override
                                    public void itemStateChanged(@NotNull ItemEvent e) {
                                        ((JetFunctionParameterTableModel)myParametersTableModel).setReceiver(
                                                e.getStateChange() == ItemEvent.SELECTED ? item.parameter : null
                                        );
                                        updateSignature();
                                        updateNameEditor();
                                    }
                                }
                        );
                        component = checkBox;
                    }
                    else
                        continue;

                    JBLabel label = new JBLabel(columnInfo.getName(), UIUtil.ComponentStyle.SMALL);
                    panel.add(label);

                    if (editor != null) {
                        editor.addDocumentListener(new DocumentAdapter() {
                            @Override
                            public void documentChanged(DocumentEvent e) {
                                fireDocumentChanged(e, columnFinal);
                            }
                        });
                        editor.setPreferredWidth(t.getWidth() / myParametersTableModel.getColumnCount());
                    }

                    components.add(component);
                    panel.add(component);
                    add(panel);
                    IJSwingUtilities.adjustComponentsOnMac(label, component);
                    column++;
                }
            }

            @Override
            public JBTableRow getValue() {
                return new JBTableRow() {
                    @Override
                    public Object getValueAt(int column) {
                        ColumnInfo columnInfo = myParametersTableModel.getColumnInfos()[column];

                        if (JetPrimaryConstructorParameterTableModel.isValVarColumn(columnInfo))
                            return ((JComboBox) components.get(column)).getSelectedItem();
                        else if (JetCallableParameterTableModel.isTypeColumn(columnInfo))
                            return item.typeCodeFragment;
                        else if (JetCallableParameterTableModel.isNameColumn(columnInfo))
                            return ((EditorTextField) components.get(column)).getText();
                        else if (JetCallableParameterTableModel.isDefaultValueColumn(columnInfo))
                            return item.defaultValueCodeFragment;
                        else
                            return null;
                    }
                };
            }

            private int getColumnWidth(int letters) {
                Font font = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
                font = new Font(font.getFontName(), font.getStyle(), 12);
                return letters * Toolkit.getDefaultToolkit().getFontMetrics(font).stringWidth("W");
            }

            private int getEditorIndex(int x) {
                int[] columnLetters = isDefaultColumnEnabled() ?
                                      new int[] { 4, getParamNamesMaxLength(), getTypesMaxLength(), getDefaultValuesMaxLength() } :
                                      new int[] { 4, getParamNamesMaxLength(), getTypesMaxLength() };
                int columnIndex = 0;

                for (int i = myMethod.getKind() == Kind.PRIMARY_CONSTRUCTOR ? 0 : 1; i < columnLetters.length; i ++) {
                    int width = getColumnWidth(columnLetters[i]);

                    if (x <= width)
                        return columnIndex;

                    columnIndex ++;
                    x -= width;
                }

                return columnIndex - 1;
            }

            @Override
            public JComponent getPreferredFocusedComponent() {
                MouseEvent me = getMouseEvent();
                int index = me != null
                            ? getEditorIndex((int) me.getPoint().getX())
                            : myMethod.getKind() == Kind.PRIMARY_CONSTRUCTOR ? 1 : 0;
                JComponent component = components.get(index);
                return component instanceof EditorTextField ? ((EditorTextField) component).getFocusTarget() : component;
            }

            @Override
            public JComponent[] getFocusableComponents() {
                JComponent[] focusable = new JComponent[components.size()];

                for (int i = 0; i < components.size(); i++) {
                    focusable[i] = components.get(i);

                    if (focusable[i] instanceof EditorTextField)
                        focusable[i] = ((EditorTextField) focusable[i]).getFocusTarget();
                }

                return focusable;
            }
        };
    }

    @Override
    protected String calculateSignature() {
        JetChangeInfo changeInfo = evaluateChangeInfo(
                myParametersTableModel,
                myReturnTypeCodeFragment,
                getMethodDescriptor(),
                getVisibility(),
                getMethodName(),
                myDefaultValueContext
        );
        return changeInfo.getNewSignature(getMethodDescriptor().getOriginalPrimaryFunction());
    }

    @Override
    protected VisibilityPanelBase<Visibility> createVisibilityControl() {
        return new ComboBoxVisibilityPanel<Visibility>(new Visibility[]{
                Visibilities.INTERNAL, Visibilities.PRIVATE, Visibilities.PROTECTED, Visibilities.PUBLIC }
        );
    }

    @Override
    protected void updateSignatureAlarmFired() {
        super.updateSignatureAlarmFired();
        validateButtons();
    }

    @Nullable
    @Override
    protected String validateAndCommitData() {
        return null;
    }

    @Override
    protected void canRun() throws ConfigurationException {
        if (myNamePanel.isVisible() && myMethod.canChangeName() && !JavaPsiFacade.getInstance(myProject).getNameHelper().isIdentifier(getMethodName()))
            throw new ConfigurationException(JetRefactoringBundle.message("function.name.is.invalid"));
        if (myMethod.canChangeReturnType() == MethodDescriptor.ReadWriteOption.ReadWrite && getReturnType() == null)
            throw new ConfigurationException(JetRefactoringBundle.message("return.type.is.invalid"));

        List<ParameterTableModelItemBase<JetParameterInfo>> parameterInfos = myParametersTableModel.getItems();

        for (ParameterTableModelItemBase<JetParameterInfo> item : parameterInfos) {
            String parameterName = item.parameter.getName();

            if (item.parameter != myParametersTableModel.getReceiver()
                && !JavaPsiFacade.getInstance(myProject).getNameHelper().isIdentifier(parameterName))
                throw new ConfigurationException(JetRefactoringBundle.message("parameter.name.is.invalid", parameterName));
            if (getType((JetTypeCodeFragment) item.typeCodeFragment) == null)
                throw new ConfigurationException(JetRefactoringBundle.message("parameter.type.is.invalid", item.typeCodeFragment.getText()));
        }
    }

    @Override
    @NotNull
    protected BaseRefactoringProcessor createRefactoringProcessor() {
        JetChangeInfo changeInfo = evaluateChangeInfo(
                myParametersTableModel,
                myReturnTypeCodeFragment,
                getMethodDescriptor(),
                getVisibility(),
                getMethodName(),
                myDefaultValueContext
        );
        return new JetChangeSignatureProcessor(myProject, changeInfo, commandName != null ? commandName : getTitle());
    }

    @NotNull
    public static BaseRefactoringProcessor createRefactoringProcessorForSilentChangeSignature(
            @NotNull Project project,
            @NotNull String commandName,
            @NotNull JetMethodDescriptor method,
            @NotNull PsiElement defaultValueContext
    ) {
        JetCallableParameterTableModel parameterTableModel = createParametersInfoModel(method, defaultValueContext);
        parameterTableModel.setParameterInfos(method.getParameters());
        JetChangeInfo changeInfo = evaluateChangeInfo(
                parameterTableModel,
                createReturnTypeCodeFragment(project, method),
                method,
                method.getVisibility(),
                method.getName(),
                defaultValueContext
        );
        return new JetChangeSignatureProcessor(project, changeInfo, commandName);
    }

    @NotNull
    public JetMethodDescriptor getMethodDescriptor() {
        return myMethod;
    }

    private static JetChangeInfo evaluateChangeInfo(
            @NotNull JetCallableParameterTableModel parametersModel,
            @Nullable PsiCodeFragment returnTypeCodeFragment,
            @NotNull JetMethodDescriptor methodDescriptor,
            @Nullable Visibility visibility,
            @NotNull String methodName,
            @NotNull PsiElement defaultValueContext
    ) {
        List<JetParameterInfo> parameters = new ArrayList<JetParameterInfo>(parametersModel.getRowCount());

        for (ParameterTableModelItemBase<JetParameterInfo> parameter : parametersModel.getItems()) {
            parameter.parameter.setCurrentTypeText(parameter.typeCodeFragment.getText().trim());

            parameters.add(parameter.parameter);

            JetExpressionCodeFragment codeFragment = (JetExpressionCodeFragment) parameter.defaultValueCodeFragment;
            JetExpression oldDefaultValue = parameter.parameter.getDefaultValueForCall();
            if (!codeFragment.getText().equals(oldDefaultValue != null ? oldDefaultValue.getText() : "")) {
                parameter.parameter.setDefaultValueForCall(codeFragment.getContentElement());
            }
        }

        String returnTypeText = returnTypeCodeFragment != null ? returnTypeCodeFragment.getText().trim() : "";
        JetMethodDescriptor descriptor = methodDescriptor instanceof JetMutableMethodDescriptor
                                         ? ((JetMutableMethodDescriptor) methodDescriptor).getOriginal()
                                         : methodDescriptor;
        JetType returnType = getType((JetTypeCodeFragment) returnTypeCodeFragment);
        return new JetChangeInfo(descriptor, methodName, returnType, returnTypeText,
                                 visibility, parameters, parametersModel.getReceiver(), defaultValueContext);
    }

    @Override
    protected int getSelectedIdx() {
        List<JetParameterInfo> parameters = myMethod.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            JetParameterInfo info = parameters.get(i);
            if (info.getIsNewParameter()) return i;
        }
        return super.getSelectedIdx();
    }
}
