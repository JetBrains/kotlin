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

package org.jetbrains.jet.resolve;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.UnresolvedReferenceDiagnosticFactory;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

/**
 * @author abreslav
 */
public abstract class ExpectedResolveData {

    protected static final String STANDARD_PREFIX = "kotlin::";

    private static class Position {
        private final PsiElement element;

        private Position(JetFile file, int offset) {
            this.element = file.findElementAt(offset);
        }

        public PsiElement getElement() {
            return element;
        }

        @Override
        public String toString() {
            return DiagnosticUtils.atLocation(element);
        }
    }

    private final Map<String, Position> declarationToPosition = Maps.newHashMap();
    private final Map<Position, String> positionToReference = Maps.newHashMap();
    private final Map<Position, String> positionToType = Maps.newHashMap();

    private final Map<String, DeclarationDescriptor> nameToDescriptor;
    private final Map<String, PsiElement> nameToPsiElement;

    @NotNull
    private final JetCoreEnvironment jetCoreEnvironment;

    public ExpectedResolveData(Map<String, DeclarationDescriptor> nameToDescriptor, Map<String, PsiElement> nameToPsiElement, @NotNull JetCoreEnvironment environment) {
        this.nameToDescriptor = nameToDescriptor;
        this.nameToPsiElement = nameToPsiElement;
        jetCoreEnvironment = environment;
    }

    public final JetFile createFileFromMarkedUpText(String fileName, String text) {
        Map<String, Integer> declarationToIntPosition = Maps.newHashMap();
        Map<Integer, String> intPositionToReference = Maps.newHashMap();
        Map<Integer, String> intPositionToType = Maps.newHashMap();

        Pattern pattern = Pattern.compile("(~[^~]+~)|(`[^`]+`)");
        while (true) {
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) break;

            String group = matcher.group();
            String name = group.substring(1, group.length() - 1);
            int start = matcher.start();
            if (group.startsWith("~")) {
                if (declarationToIntPosition.put(name, start) != null) {
                    throw new IllegalArgumentException("Redeclaration: " + name);
                }
            }
            else if (group.startsWith("`")) {
                if (name.startsWith(":")) {
                    intPositionToType.put(start - 1, name.substring(1));
                }
                else {
                    intPositionToReference.put(start, name);
                }
            }
            else {
                throw new IllegalStateException();
            }

            text = text.substring(0, start) + text.substring(matcher.end());
        }

        JetFile jetFile = createJetFile(fileName, text);

        for (Map.Entry<Integer, String> entry : intPositionToType.entrySet()) {
            positionToType.put(new Position(jetFile, entry.getKey()), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : declarationToIntPosition.entrySet()) {
            declarationToPosition.put(entry.getKey(), new Position(jetFile, entry.getValue()));
        }
        for (Map.Entry<Integer, String> entry : intPositionToReference.entrySet()) {
            positionToReference.put(new Position(jetFile, entry.getKey()), entry.getValue());
        }
        return jetFile;
    }

    protected abstract JetFile createJetFile(String fileName, String text);

    public final void checkResult(List<JetFile> files) {
        if (files.isEmpty()) {
            System.err.println("Suspicious: no files");
            return;
        }
        final Set<PsiElement> unresolvedReferences = Sets.newHashSet();
        Project project = files.iterator().next().getProject();
        JetStandardLibrary lib = JetStandardLibrary.getInstance();

        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(project, files,
                Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY,
                jetCoreEnvironment.getCompilerDependencies());
        BindingContext bindingContext = analyzeExhaust.getBindingContext();
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            if (diagnostic.getFactory() instanceof UnresolvedReferenceDiagnosticFactory) {
                unresolvedReferences.add(diagnostic.getPsiElement());
            }
        }

        Map<String, PsiElement> nameToDeclaration = Maps.newHashMap();

        Map<PsiElement, String> declarationToName = Maps.newHashMap();
        for (Map.Entry<String, Position> entry : declarationToPosition.entrySet()) {
            String name = entry.getKey();
            Position position = entry.getValue();
            PsiElement element = position.getElement();

            PsiElement ancestorOfType;

            if (name.equals("file")) {
                ancestorOfType = element.getContainingFile();
            }
            else {
                ancestorOfType = getAncestorOfType(JetDeclaration.class, element);
                if (ancestorOfType == null) {
                    JetNamespaceHeader header = getAncestorOfType(JetNamespaceHeader.class, element);
                    assert header != null : "Not a declaration: " + name;
                    ancestorOfType = element;
                }
            }
            nameToDeclaration.put(name, ancestorOfType);
            declarationToName.put(ancestorOfType, name);
        }

        for (Map.Entry<Position, String> entry : positionToReference.entrySet()) {
            Position position = entry.getKey();
            String name = entry.getValue();
            PsiElement element = position.getElement();

            JetReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(element, JetReferenceExpression.class);
            if ("!".equals(name)) {
                assertTrue(
                        "Must have been unresolved: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + renderNullableDescriptor(bindingContext.get(REFERENCE_TARGET, referenceExpression)),
                        unresolvedReferences.contains(referenceExpression));
                continue;
            }
            if ("!!".equals(name)) {
                assertTrue(
                        "Must have been resolved to multiple descriptors: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + renderNullableDescriptor(bindingContext.get(REFERENCE_TARGET, referenceExpression)),
                        bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, referenceExpression) != null);
                continue;
            }
            else if ("!null".equals(name)) {
                assertTrue(
                       "Must have been resolved to null: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + renderNullableDescriptor(bindingContext.get(REFERENCE_TARGET, referenceExpression)),
                        bindingContext.get(REFERENCE_TARGET, referenceExpression) == null
                );
                continue;
            }
            else if ("!error".equals(name)) {
                assertTrue(
                       "Must have been resolved to error: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + renderNullableDescriptor(bindingContext.get(REFERENCE_TARGET, referenceExpression)),
                       ErrorUtils.isError(bindingContext.get(REFERENCE_TARGET, referenceExpression))
                );
                continue;
            }

            PsiElement expected = nameToDeclaration.get(name);
            if (expected == null && name.startsWith("$")) {
                expected = nameToDeclaration.get(name.substring(1));
            }
            if (expected == null) {
                expected = nameToPsiElement.get(name);
            }

            JetReferenceExpression reference = getAncestorOfType(JetReferenceExpression.class, element);
            if (expected == null && name.startsWith(STANDARD_PREFIX)) {
                DeclarationDescriptor expectedDescriptor = nameToDescriptor.get(name);
                JetTypeReference typeReference = getAncestorOfType(JetTypeReference.class, element);
                if (expectedDescriptor != null) {
                    DeclarationDescriptor actual = bindingContext.get(REFERENCE_TARGET, reference);
                    assertSame("Expected: " + name, expectedDescriptor.getOriginal(), actual == null
                                                                                      ? null
                                                                                      : actual.getOriginal());
                    continue;
                }

                JetType actualType = bindingContext.get(BindingContext.TYPE, typeReference);
                assertNotNull("Type " + name + " not resolved for reference " + name, actualType);
                ClassifierDescriptor expectedClass = lib.getLibraryScope().getClassifier(name.substring(STANDARD_PREFIX.length()));
                assertNotNull("Expected class not found: " + name);
                assertSame("Type resolution mismatch: ", expectedClass.getTypeConstructor(), actualType.getConstructor());
                continue;
            }
            assert expected != null : "No declaration for " + name;

            PsiElement actual = BindingContextUtils.resolveToDeclarationPsiElement(bindingContext, reference);
            if (actual instanceof JetSimpleNameExpression) {
                actual = ((JetSimpleNameExpression)actual).getIdentifier();
            }

            String actualName = null;
            if (actual != null) {
                actualName = declarationToName.get(actual);
                if (actualName == null) {
                    actualName = actual.toString();
                }
            }
            assertNotNull(element.getText(), reference);

            if (expected instanceof JetParameter || actual instanceof JetParameter) {
                DeclarationDescriptor expectedDescriptor;
                if (name.startsWith("$")) {
                    expectedDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, (JetParameter) expected);
                }
                else {
                    expectedDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expected);
                    if (expectedDescriptor == null) {
                        expectedDescriptor = bindingContext.get(BindingContext.CONSTRUCTOR, (JetElement) expected);
                    }
                }


                DeclarationDescriptor actualDescriptor = bindingContext.get(REFERENCE_TARGET, reference);

                assertEquals(
                        "Reference `" + name + "`" + renderReferenceInContext(reference) + " is resolved into " + actualName + ".",
                        expectedDescriptor, actualDescriptor);
            }
            else {
                assertEquals(
                        "Reference `" + name + "`" + renderReferenceInContext(reference) + " is resolved into " + actualName + ".",
                        expected, actual);
            }
        }

        for (Map.Entry<Position, String> entry : positionToType.entrySet()) {
            Position position = entry.getKey();
            String typeName = entry.getValue();

            PsiElement element = position.getElement();
            JetExpression expression = getAncestorOfType(JetExpression.class, element);

            JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
            TypeConstructor expectedTypeConstructor;
            if (typeName.startsWith(STANDARD_PREFIX)) {
                String name = typeName.substring(STANDARD_PREFIX.length());
                ClassifierDescriptor expectedClass = lib.getLibraryScope().getClassifier(name);

                assertNotNull("Expected class not found: " + typeName, expectedClass);
                expectedTypeConstructor = expectedClass.getTypeConstructor();
            }
            else {
                Position declarationPosition = declarationToPosition.get(typeName);
                assertNotNull("Undeclared: " + typeName, declarationPosition);
                PsiElement declElement = declarationPosition.getElement();
                assertNotNull(declarationPosition);
                JetDeclaration declaration = getAncestorOfType(JetDeclaration.class, declElement);
                assertNotNull(declaration);
                if (declaration instanceof JetClass) {
                    ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, declaration);
                    expectedTypeConstructor = classDescriptor.getTypeConstructor();
                }
                else if (declaration instanceof JetTypeParameter) {
                    TypeParameterDescriptor typeParameterDescriptor = bindingContext.get(BindingContext.TYPE_PARAMETER, (JetTypeParameter) declaration);
                    expectedTypeConstructor = typeParameterDescriptor.getTypeConstructor();
                }
                else {
                    fail("Unsupported declaration: " + declaration);
                    return;
                }
            }

            assertNotNull(expression.getText() + " type is null", expressionType);
            assertSame("At " + position + ": ", expectedTypeConstructor, expressionType.getConstructor());
        }
    }

    private static String renderReferenceInContext(JetReferenceExpression referenceExpression) {
        JetExpression statement = referenceExpression;
        while (true) {
            PsiElement parent = statement.getParent();
            if (!(parent instanceof JetExpression)) break;
            if (parent instanceof JetBlockExpression) break;
            statement = (JetExpression) parent;
        }
        JetDeclaration declaration = PsiTreeUtil.getParentOfType(referenceExpression, JetDeclaration.class);



        return referenceExpression.getText() + " at " + DiagnosticUtils.atLocation(referenceExpression) +
                                    " in " + statement.getText() + (declaration == null ? "" : " in " + declaration.getText());
    }

    private static <T> T getAncestorOfType(Class<T> type, PsiElement element) {
        while (element != null && !type.isInstance(element)) {
            element = element.getParent();
        }
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        T result = (T) element;
        return result;
    }

    @NotNull
    private static String renderNullableDescriptor(@Nullable DeclarationDescriptor d) {
        return d == null ? "<null>" : DescriptorRenderer.TEXT.render(d);
    }
}
