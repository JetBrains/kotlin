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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.project.PluginJetFilesProvider;

import java.util.Collection;

public class MigrateSureInProjectFix extends JetIntentionAction<PsiElement> {
    public MigrateSureInProjectFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("migrate.sure");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("migrate.sure");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file)
               && (file instanceof JetFile)
               && isUnresolvedSure(element);
    }

    private static boolean isUnresolvedSure(PsiElement element) {
        if (!(element instanceof JetSimpleNameExpression)) return false;
        JetSimpleNameExpression calleeExpression = (JetSimpleNameExpression) element;

        // it must be "sure"
        if (!"sure".equals(calleeExpression.getReferencedName())) return false;

        // parent must be a call expression, and this must be the callee
        PsiElement parent = calleeExpression.getParent();
        if (!(parent instanceof JetCallExpression)) return false;
        JetCallExpression callExpression = (JetCallExpression) parent;
        if (callExpression.getCalleeExpression() != calleeExpression) return false;

        // it must be a qualified call
        PsiElement callParent = callExpression.getParent();
        assert callParent != null;
        if (!(callParent instanceof JetDotQualifiedExpression)) return false;

        // not more than one type argument
        if (callExpression.getTypeArguments().size() > 1) return false;

        // no value arguments
        if (!callExpression.getValueArguments().isEmpty() || !callExpression.getFunctionLiteralArguments().isEmpty()) return false;

        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        Collection<JetFile> files = PluginJetFilesProvider.allFilesInProject(project);

        for (JetFile jetFile : files) {
            replaceUnresolvedSure(jetFile);
        }
    }

    private void replaceUnresolvedSure(JetFile file) {
        final BindingContext context = ResolvePackage.getBindingContext(file);
        for (JetDeclaration declaration : file.getDeclarations()) {
            declaration.acceptChildren(new JetVisitorVoid() {

                @Override
                public void visitCallExpression(@NotNull JetCallExpression expression) {
                    expression.acceptChildren(this);

                    if (!isUnresolvedSure(expression.getCalleeExpression())) return;

                    PsiElement parent = expression.getParent();
                    assert parent != null : "isUnresolvedSure() must be true";
                    JetExpression receiver = ((JetDotQualifiedExpression) parent).getReceiverExpression();

                    // sure() must be unresolved
                    DeclarationDescriptor callee = context.get(BindingContext.REFERENCE_TARGET,
                                                               (JetReferenceExpression) expression.getCalleeExpression());
                    if (callee != null) return;

                    // replace 'foo.sure()' with 'foo!!'
                    JetExpression newExpression = JetPsiFactory.createExpression(expression.getProject(), receiver.getText() + "!!");
                    parent.replace(newExpression);
                }

                @Override
                public void visitElement(PsiElement element) {
                    element.acceptChildren(this);
                }
            });
        }
    }


    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                return new MigrateSureInProjectFix(element);
            }
        };
    }
}
