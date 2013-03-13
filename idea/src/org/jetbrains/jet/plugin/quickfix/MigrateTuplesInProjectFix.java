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

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BodiesResolveContext;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeProvider;
import org.jetbrains.jet.plugin.project.PluginJetFilesProvider;

import java.util.Collection;
import java.util.Collections;

@Deprecated // Tuples are to be removed in Kotlin M4
public class MigrateTuplesInProjectFix extends JetIntentionAction<PsiElement> {
    public MigrateTuplesInProjectFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("migrate.tuple");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("migrate.tuple");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file)
               && (file instanceof JetFile)
               && (element instanceof JetTupleExpression || element instanceof JetTupleType);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetFile initialFile = (JetFile) file;
        Collection<JetFile> files = PluginJetFilesProvider.WHOLE_PROJECT_DECLARATION_PROVIDER.fun(initialFile);

        AnalyzeExhaust exhaust = analyzeFiles(initialFile, files);

        for (JetFile jetFile : files) {
            replaceTupleComponentCalls(jetFile, exhaust.getBindingContext());
            replaceTuple(jetFile);
        }
    }

    public static AnalyzeExhaust analyzeFiles(JetFile initialFile, Collection<JetFile> files) {
        AnalyzeExhaust analyzeExhaustHeaders = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(initialFile).analyzeFiles(
                initialFile.getProject(),
                files,
                Collections.<AnalyzerScriptParameter>emptyList(),
                Predicates.<PsiFile>alwaysFalse());

        BodiesResolveContext context = analyzeExhaustHeaders.getBodiesResolveContext();
        ModuleConfiguration moduleConfiguration = analyzeExhaustHeaders.getModuleConfiguration();
        assert context != null : "Headers resolver should prepare and stored information for bodies resolve";

        // Need to resolve bodies in given file and all in the same package
        return AnalyzerFacadeProvider.getAnalyzerFacadeForFile(initialFile).analyzeBodiesInFiles(
                initialFile.getProject(),
                Collections.<AnalyzerScriptParameter>emptyList(),
                Predicates.<PsiFile>alwaysTrue(),
                new DelegatingBindingTrace(analyzeExhaustHeaders.getBindingContext(), "trace in migrate tuples fix"),
                context,
                moduleConfiguration);
    }

    private void replaceTupleComponentCalls(JetFile file, final BindingContext context) {
        for (JetDeclaration declaration : file.getDeclarations()) {
            declaration.acceptChildren(new JetVisitorVoid() {

                @Override
                public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
                    DeclarationDescriptor referenceTarget = context.get(BindingContext.REFERENCE_TARGET, expression);
                    if (referenceTarget == null) return;

                    DeclarationDescriptor containingDeclaration = referenceTarget.getContainingDeclaration();
                    if (containingDeclaration == null) return;

                    ImmutableSet<ClassDescriptor> supportedTupleClasses =
                            ImmutableSet.of(KotlinBuiltIns.getInstance().getTuple(2), KotlinBuiltIns.getInstance().getTuple(3));
                    //noinspection SuspiciousMethodCalls
                    if (!supportedTupleClasses.contains(containingDeclaration)) return;

                    ImmutableMap<String, String> supportedComponents = ImmutableMap.<String, String>builder()
                            .put("_1", "first")
                            .put("_2", "second")
                            .put("_3", "third")
                            .build();
                    String newName = supportedComponents.get(expression.getReferencedName());
                    if (newName == null) return;

                    expression.replace(JetPsiFactory.createExpression(expression.getProject(), newName));
                }

                @Override
                public void visitElement(PsiElement element) {
                    element.acceptChildren(this);
                }
            });
        }
    }

    private void replaceTuple(JetFile file) {
        for (JetDeclaration declaration : file.getDeclarations()) {
            declaration.acceptChildren(new JetVisitorVoid() {

                @Override
                public void visitTupleExpression(JetTupleExpression expression) {
                    expression.acceptChildren(this);

                    int size = expression.getEntries().size();
                    switch (size) {
                        case 0:
                            expression.replace(JetPsiFactory.createExpression(expression.getProject(), "Unit.VALUE"));
                            return;
                        case 1:
                            expression.replace(expression.getEntries().get(0));
                            return;

                        case 2:
                            replaceWithExpression(expression, "Pair");
                            return;

                        case 3:
                            replaceWithExpression(expression, "Triple");
                            return;

                        default:
                            // Can't do anything
                    }
                }

                @Override
                public void visitTupleType(JetTupleType type) {
                    type.acceptChildren(this);

                    int size = type.getComponentTypeRefs().size();
                    switch (size) {
                        case 0:
                            type.replace(JetPsiFactory.createType(type.getProject(), "Unit").getTypeElement());
                            return;
                        case 1:
                            type.replace(type.getComponentTypeRefs().get(0).getTypeElement());
                            return;

                        case 2:
                            replaceWithType(type, "Pair");
                            return;

                        case 3:
                            replaceWithType(type, "Triple");
                            return;

                        default:
                            // Can't do anything
                    }
                }

                @Override
                public void visitElement(PsiElement element) {
                    element.acceptChildren(this);
                }
            });
        }
    }

    private static void replaceWithExpression(JetTupleExpression expression, String name) {
        String tupleContents = getTupleContents(expression.getText());
        if (tupleContents == null) return;

        String newText = name + "(" + tupleContents + ")";
        JetExpression newExpression = JetPsiFactory.createExpression(expression.getProject(), newText);

        expression.replace(newExpression);
    }

    private static void replaceWithType(JetTupleType type, String name) {
        String tupleContents = getTupleContents(type.getText());
        if (tupleContents == null) return;

        String newText = name + "<" + tupleContents + ">";
        JetTypeReference newExpression = JetPsiFactory.createType(type.getProject(), newText);

        type.replace(newExpression.getTypeElement());
    }

    @Nullable
    private static String getTupleContents(String text) {
        int indexOfHash = text.indexOf("#(");
        assert indexOfHash >= 0;
        String noOpenPar = text.substring(indexOfHash + 2);
        int lastClosingPar = noOpenPar.lastIndexOf(')');
        if (lastClosingPar < 0) {
            return null;
        }
        return noOpenPar.substring(0, lastClosingPar);
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                return new MigrateTuplesInProjectFix(element);
            }
        };
    }
}
