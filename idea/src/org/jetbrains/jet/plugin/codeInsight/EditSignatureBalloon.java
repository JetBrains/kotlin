/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
* @author Evgeny Gerashchenko
* @since 10.08.12
*/
@SuppressWarnings("SSBasedInspection")
class EditSignatureBalloon {
    private final Editor editor;
    private final PsiMethod method;
    private final String previousSignature;
    private final Balloon balloon;

    public EditSignatureBalloon(@NotNull PsiMethod method, @NotNull String previousSignature) {
        this.method = method;
        this.previousSignature = previousSignature;

        editor = createEditor();
        JPanel panel = createBalloonPanel();
        balloon = createBalloon(panel);
    }

    private Balloon createBalloon(JPanel panel) {
        BalloonBuilder builder = JBPopupFactory.getInstance().createDialogBalloonBuilder(panel, "Kotlin signature");
        builder.setHideOnClickOutside(true);
        builder.setHideOnKeyOutside(true);

        Balloon balloon = builder.createBalloon();
        balloon.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                dispose();
            }
        });
        return balloon;
    }

    private JPanel createBalloonPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                return new Dimension((int) (preferredSize.width * 1.4), preferredSize.height);
            }
        };
        panel.add(editor.getComponent(), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save") {
            @Override
            public boolean isDefaultButton() {
                return true;
            }
        };
        toolbar.add(saveButton);
        panel.add(toolbar, BorderLayout.SOUTH);

        ActionListener saveAndHideListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndHide();
            }
        };

        ActionListener cancelActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                balloon.hide();
            }
        };

        saveButton.addActionListener(saveAndHideListener);
        panel.registerKeyboardAction(saveAndHideListener, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
            SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK),
                                     JComponent.WHEN_IN_FOCUSED_WINDOW);
        panel.registerKeyboardAction(cancelActionListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                     JComponent.WHEN_IN_FOCUSED_WINDOW);

        return panel;
    }

    private Editor createEditor() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        assert editorFactory != null;
        Document document = editorFactory.createDocument(this.previousSignature);

        Editor editor = editorFactory.createEditor(document, this.method.getProject(), JetFileType.INSTANCE, false);
        EditorSettings settings = editor.getSettings();
        settings.setVirtualSpace(false);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setRightMarginShown(false);
        settings.setAdditionalPageAtBottom(false);
        settings.setAdditionalLinesCount(2);

        if (editor instanceof EditorEx) {
            ((EditorEx)editor).setEmbeddedIntoDialogWrapper(true);
        }

        editor.getColorsScheme().setColor(EditorColors.CARET_ROW_COLOR, editor.getColorsScheme().getDefaultBackground());

        return editor;
    }

    private int getLineY(@Nullable DataContext dataContext) {
        if (dataContext != null) {
            Editor mainEditor = PlatformDataKeys.EDITOR.getData(dataContext);
            Document document = PsiDocumentManager.getInstance(method.getProject()).getDocument(method.getContainingFile());
            if (mainEditor != null && document != null) {
                int lineNumber = document.getLineNumber(method.getTextOffset());
                return mainEditor.logicalPositionToXY(new LogicalPosition(lineNumber, 0)).y;
            }
        }
        return Integer.MAX_VALUE;
    }

    public void show(MouseEvent e) {
        Point eventPoint = e.getPoint();
        int lineY = getLineY(DataManager.getInstance().getDataContext(e.getComponent()));
        Point point = new Point(eventPoint.x, Math.min(lineY, eventPoint.y));
        balloon.show(new RelativePoint(e.getComponent(), point), Balloon.Position.above);
        IdeFocusManager.getInstance(editor.getProject()).requestFocus(editor.getContentComponent(), false);
    }

    private void dispose() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        assert editorFactory != null;
        editorFactory.releaseEditor(editor);
    }

    private void saveAndHide() {
        String newSignature = editor.getDocument().getText();
        if (!previousSignature.equals(newSignature)) {
            final Project project = method.getProject();
            final PsiNameValuePair[] nameValuePairs = JavaPsiFacade.getElementFactory(project).createAnnotationFromText(
                    "@" + KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION + "(value=\"" + newSignature + "\")", null)
                    .getParameterList().getAttributes();

            new WriteCommandAction(project) {
                @Override
                protected void run(final Result result) throws Throwable {
                    ExternalAnnotationsManager
                            .getInstance(project).deannotate(method, KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION);
                    ExternalAnnotationsManager.getInstance(project).annotateExternally(
                            method, KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION, method.getContainingFile(),
                            nameValuePairs);
                }
            }.execute();
        }

        balloon.hide();
    }
}
