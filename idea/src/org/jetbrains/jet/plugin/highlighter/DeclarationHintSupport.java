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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseMotionAdapter;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.LightweightHint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.ProjectRootsUtil;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.configuration.JetModuleTypeManager;
import org.jetbrains.jet.plugin.util.LongRunningReadTask;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.swing.*;
import java.awt.*;

public class DeclarationHintSupport extends AbstractProjectComponent {
    private static final Logger LOG = Logger.getInstance(DeclarationHintSupport.class.getName());

    private final MouseMoveListener mouseMoveListener = new MouseMoveListener();

    public DeclarationHintSupport(Project project) {
        super(project);
    }

    @Override
    public void initComponent() {
        EditorFactory instance = EditorFactory.getInstance();
        if (instance != null) {
            instance.getEventMulticaster().addEditorMouseMotionListener(mouseMoveListener, myProject);
        }
        else {
            LOG.error("Couldn't initialize " + DeclarationHintSupport.class.getName() + " component");
        }
    }

    private class MouseMoveListener extends EditorMouseMotionAdapter {
        private JetNamedDeclaration lastNamedDeclaration = null;
        private ProgressIndicator lastIndicator;
        private LightweightHint lastHint;

        @Override
        public void mouseMoved(EditorMouseEvent e) {
            if (DumbService.getInstance(myProject).isDumb() || !myProject.isInitialized()) {
                return;
            }

            if (e.isConsumed() || e.getArea() != EditorMouseEventArea.EDITING_AREA) {
                return;
            }

            Editor editor = e.getEditor();
            PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
            if (psiFile == null || psiFile.getLanguage() != JetLanguage.INSTANCE ||
                    !ProjectRootsUtil.isInSource(psiFile) ||
                    JetModuleTypeManager.getInstance().isKtFileInGradleProjectInWrongFolder(psiFile)) {
                return;
            }

            JetFile jetFile = (JetFile) psiFile;

            int offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(e.getMouseEvent().getPoint()));
            PsiElement elementAtCursor = psiFile.findElementAt(offset);
            JetNamedDeclaration declaration = PsiTreeUtil.getParentOfType(elementAtCursor, JetNamedDeclaration.class);

            if (declaration != null && declaration.getNameIdentifier() == elementAtCursor) {
                if (lastNamedDeclaration == declaration) {
                    return;
                }

                if (lastIndicator != null) {
                    lastIndicator.cancel();
                }

                lastNamedDeclaration = declaration;
                lastIndicator = new ProgressIndicatorBase();

                // Move long resolve operation to another thread
                searchResolvedDescriptor(jetFile, declaration, editor, lastIndicator);
            }
            else {
                // Mouse moved out of the declaration - can stop search for descriptor
                if (lastIndicator != null) {
                    lastIndicator.cancel();
                }

                if (lastHint != null) {
                    lastHint.hide();
                }

                lastHint = null;
                lastNamedDeclaration = null;
                lastIndicator = null;
            }
        }

        // Executed in Event dispatch thread
        private void onDescriptorReady(
                @Nullable DeclarationDescriptor descriptor,
                @NotNull JetNamedDeclaration declaration,
                @NotNull Editor editor,
                @NotNull PsiFile psiFile,
                @NotNull ProgressIndicator indicator
        ) {
            if (indicator != lastIndicator) {
                // This is callback for some outdated request
                return;
            }

            // Clear - last request is done
            lastNamedDeclaration = null;
            lastIndicator = null;

            if (descriptor == null) {
                return;
            }

            if (editor.isDisposed()) {
                return;
            }

            // Check that cursor is still at the same position
            Point mousePosition = editor.getContentComponent().getMousePosition();
            if (mousePosition == null) {
                return;
            }

            int offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(mousePosition));
            PsiElement elementAtCursor = psiFile.findElementAt(offset);

            if (elementAtCursor != declaration.getNameIdentifier()) {
                return;
            }

            JLayeredPane layeredPane = editor.getComponent().getRootPane().getLayeredPane();
            Point point = SwingUtilities.convertPoint(editor.getContentComponent(), mousePosition, layeredPane);
            lastHint = new LightweightHint(HintUtil.createInformationLabel(DescriptorRenderer.HTML.render(descriptor)));
            ((HintManagerImpl) HintManager.getInstance()).showEditorHint(
                    lastHint, editor, point,
                    HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE
                    | HintManager.HIDE_BY_SCROLLING | HintManager.HIDE_BY_OTHER_HINT | HintManager.HIDE_BY_MOUSEOVER, 0, false);
        }

        // Moves execution from GUI thread to thread pool thread
        private void searchResolvedDescriptor(
                @NotNull final JetFile jetFile,
                @NotNull final JetNamedDeclaration declaration,
                @NotNull final Editor editor,
                @NotNull final ProgressIndicator indicator
        ) {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    LongRunningReadTask.runWithWriteActionPriority(indicator, new Runnable() {
                        @Override
                        public void run() {
                            // Executed in the thread from Thread Pool
                            DeclarationDescriptor descriptor = null;
                            try {
                                BindingContext bindingContext =
                                        ResolvePackage.getBindingContext(jetFile);
                                descriptor = bindingContext.getDiagnostics().forElement(declaration).isEmpty()
                                             ? bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
                                             : null;
                            }
                            finally {
                                // Back to GUI thread for submitting result
                                final DeclarationDescriptor finalDescriptor = descriptor;
                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        onDescriptorReady(finalDescriptor, declaration, editor, jetFile, indicator);
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }
    }
}
