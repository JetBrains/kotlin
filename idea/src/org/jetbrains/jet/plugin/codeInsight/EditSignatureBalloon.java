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
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupAdapter;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
* @author Evgeny Gerashchenko
* @since 10.08.12
*/
@SuppressWarnings("SSBasedInspection")
class EditSignatureBalloon {
    private final Editor editor;
    private final PsiMethod method;
    private final Project project;
    private final String previousSignature;
    private final Balloon balloon;

    public EditSignatureBalloon(@NotNull PsiMethod method, @NotNull String previousSignature) {
        this.method = method;
        project = method.getProject();
        this.previousSignature = previousSignature;

        editor = createEditor();
        JPanel panel = createBalloonPanel();
        balloon = createBalloon(panel);
    }

    private Balloon createBalloon(JPanel panel) {
        Balloon balloon = JBPopupFactory.getInstance().createDialogBalloonBuilder(panel, "Kotlin signature")
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .setBlockClicksThroughBalloon(true).createBalloon();

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
        JButton deleteButton = new JButton("Delete");

        toolbar.add(saveButton);
        toolbar.add(deleteButton);
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
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteAndHide();
            }
        });
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

        Editor editor = editorFactory.createEditor(document, project, JetFileType.INSTANCE, false);
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

    private int getLineY(@Nullable Editor editor) {
        if (editor != null) {
            Document document = PsiDocumentManager.getInstance(project).getDocument(method.getContainingFile());
            if (document != null) {
                int lineNumber = document.getLineNumber(method.getTextOffset());
                return editor.logicalPositionToXY(new LogicalPosition(lineNumber, 0)).y;
            }
        }
        return Integer.MAX_VALUE;
    }

    public void show(@Nullable Point point, @NotNull Editor editor) {
        int lineY = getLineY(editor);
        EditorGutterComponentEx gutter = (EditorGutterComponentEx) editor.getGutter();
        Point adjustedPoint;
        if (point == null) {
            adjustedPoint = new Point(gutter.getIconsAreaWidth() + gutter.getLineMarkerAreaOffset(), lineY);
        }
        else {
            adjustedPoint = new Point(point.x, Math.min(lineY, point.y));
        }
        balloon.show(new RelativePoint(gutter, adjustedPoint), Balloon.Position.above);
        IdeFocusManager.getInstance(editor.getProject()).requestFocus(editor.getContentComponent(), false);
    }

    private void dispose() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        assert editorFactory != null;
        editorFactory.releaseEditor(editor);
    }

    private void deleteAndHide() {
        new WriteCommandAction(project) {
            @Override
            protected void run(final Result result) throws Throwable {
                ExternalAnnotationsManager.getInstance(project)
                        .deannotate(method, KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION);
            }
        }.execute();
        KotlinSignatureInJavaMarkerProvider.refresh(project);

        balloon.hide();
    }

    private void saveAndHide() {
        final String newSignature = editor.getDocument().getText();
        if (!previousSignature.equals(newSignature)) {
            new WriteCommandAction(project) {
                @Override
                protected void run(final Result result) throws Throwable {
                    ExternalAnnotationsManager
                            .getInstance(project).deannotate(method, KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION);
                    ExternalAnnotationsManager.getInstance(project).annotateExternally(
                            method, KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION, method.getContainingFile(),
                            signatureToNameValuePairs(project, newSignature));
                }
            }.execute();
        }

        balloon.hide();
    }

    static PsiNameValuePair[] signatureToNameValuePairs(@NotNull Project project, @NotNull String signature) {
        return JavaPsiFacade.getElementFactory(project).createAnnotationFromText(
                "@" + KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION
                + "(value=\"" + StringUtil.escapeStringCharacters(signature) + "\")", null).getParameterList().getAttributes();
    }
}
