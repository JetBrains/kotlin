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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

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
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(editor.getComponent(), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        toolbar.add(saveButton);
        panel.add(toolbar, BorderLayout.SOUTH);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndHide();
            }
        });
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
        settings.setAdditionalLinesCount(0);

        return editor;
    }

    public void show(MouseEvent e) {
        balloon.show(new RelativePoint(e), Balloon.Position.above);
        IdeFocusManager.getInstance(editor.getProject()).requestFocus(editor.getContentComponent(), false);
    }

    private void dispose() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        assert editorFactory != null;
        editorFactory.releaseEditor(editor);
    }

    private void saveAndHide() {
        String newSignature = editor.getDocument().getText();
        if (previousSignature.equals(newSignature)) return;
        final Project project = method.getProject();
        final PsiNameValuePair[] nameValuePairs = JavaPsiFacade.getElementFactory(project).createAnnotationFromText(
                "@" + KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION + "(value=\"" + newSignature + "\")", null).getParameterList().getAttributes();

        new WriteCommandAction(project){
            @Override
            protected void run(final Result result) throws Throwable {
                ExternalAnnotationsManager
                        .getInstance(project).deannotate(method, KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION);
                ExternalAnnotationsManager.getInstance(project).annotateExternally(
                        method, KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION, method.getContainingFile(), nameValuePairs);
            }
        }.execute();

        balloon.hide();
    }
}
