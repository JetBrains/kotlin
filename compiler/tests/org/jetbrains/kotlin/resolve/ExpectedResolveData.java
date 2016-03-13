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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiQualifiedNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET;
import static org.junit.Assert.*;

public abstract class ExpectedResolveData {

    protected static final String STANDARD_PREFIX = "kotlin::";

    private static class Position {
        private final PsiElement element;

        private Position(KtFile file, int offset) {
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

    public ExpectedResolveData(Map<String, DeclarationDescriptor> nameToDescriptor, Map<String, PsiElement> nameToPsiElement) {
        this.nameToDescriptor = nameToDescriptor;
        this.nameToPsiElement = nameToPsiElement;
    }

    public final KtFile createFileFromMarkedUpText(String fileName, String text) {
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

        KtFile ktFile = createKtFile(fileName, text);

        for (Map.Entry<Integer, String> entry : intPositionToType.entrySet()) {
            positionToType.put(new Position(ktFile, entry.getKey()), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : declarationToIntPosition.entrySet()) {
            declarationToPosition.put(entry.getKey(), new Position(ktFile, entry.getValue()));
        }
        for (Map.Entry<Integer, String> entry : intPositionToReference.entrySet()) {
            positionToReference.put(new Position(ktFile, entry.getKey()), entry.getValue());
        }
        return ktFile;
    }

    protected abstract KtFile createKtFile(String fileName, String text);

    protected static BindingContext analyze(List<KtFile> files, KotlinCoreEnvironment environment) {
        if (files.isEmpty()) {
            System.err.println("Suspicious: no files");
            return BindingContext.EMPTY;
        }

        Project project = files.iterator().next().getProject();
        AnalysisResult analysisResult = JvmResolveUtil.analyzeFilesWithJavaIntegration(project, files, environment);
        return analysisResult.getBindingContext();
    }

    public final void checkResult(BindingContext bindingContext) {
        Set<PsiElement> unresolvedReferences = Sets.newHashSet();
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
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
                ancestorOfType = getAncestorOfType(KtDeclaration.class, element);
                if (ancestorOfType == null) {
                    KtPackageDirective directive = getAncestorOfType(KtPackageDirective.class, element);
                    assert directive != null : "Not a declaration: " + name;
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

            KtReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(element, KtReferenceExpression.class);
            DeclarationDescriptor referenceTarget = bindingContext.get(REFERENCE_TARGET, referenceExpression);
            if ("!".equals(name)) {
                assertTrue(
                        "Must have been unresolved: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + renderNullableDescriptor(referenceTarget),
                        unresolvedReferences.contains(referenceExpression));

                assertTrue(
                        String.format("Reference =%s= has a reference target =%s= but expected to be unresolved",
                                      renderReferenceInContext(referenceExpression), renderNullableDescriptor(referenceTarget)),
                        referenceTarget == null);

                continue;
            }
            if ("!!".equals(name)) {
                assertTrue(
                        "Must have been resolved to multiple descriptors: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + renderNullableDescriptor(referenceTarget),
                        bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, referenceExpression) != null);
                continue;
            }
            else if ("!null".equals(name)) {
                assertTrue(
                       "Must have been resolved to null: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + renderNullableDescriptor(referenceTarget),
                        referenceTarget == null
                );
                continue;
            }
            else if ("!error".equals(name)) {
                assertTrue(
                       "Must have been resolved to error: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + renderNullableDescriptor(referenceTarget),
                       ErrorUtils.isError(referenceTarget)
                );
                continue;
            }

            PsiElement expected = nameToDeclaration.get(name);
            if (expected == null) {
                expected = nameToPsiElement.get(name);
            }

            KtReferenceExpression reference = getAncestorOfType(KtReferenceExpression.class, element);
            if (expected == null && name.startsWith(STANDARD_PREFIX)) {
                DeclarationDescriptor expectedDescriptor = nameToDescriptor.get(name);
                KtTypeReference typeReference = getAncestorOfType(KtTypeReference.class, element);
                if (expectedDescriptor != null) {
                    DeclarationDescriptor actual = bindingContext.get(REFERENCE_TARGET, reference);
                    assertSame("Expected: " + name, expectedDescriptor.getOriginal(), actual == null
                                                                                      ? null
                                                                                      : actual.getOriginal());
                    continue;
                }

                KotlinType actualType = bindingContext.get(BindingContext.TYPE, typeReference);
                assertNotNull("Type " + name + " not resolved for reference " + name, actualType);
                ClassifierDescriptor expectedClass = getBuiltinClass(name.substring(STANDARD_PREFIX.length()));
                assertSame("Type resolution mismatch: ", expectedClass.getTypeConstructor(), actualType.getConstructor());
                continue;
            }
            assert expected != null : "No declaration for " + name;

            if (referenceTarget instanceof PackageViewDescriptor) {
                KtPackageDirective expectedDirective = PsiTreeUtil.getParentOfType(expected, KtPackageDirective.class);
                FqName expectedFqName;
                if (expectedDirective != null) {
                    expectedFqName = expectedDirective.getFqName();
                }
                else if (expected instanceof PsiQualifiedNamedElement) {
                    String qualifiedName = ((PsiQualifiedNamedElement) expected).getQualifiedName();
                    assert qualifiedName != null : "No qualified name for " + name;
                    expectedFqName = new FqName(qualifiedName);
                }
                else {
                    throw new IllegalStateException(expected.getClass().getName() + " name=" + name);
                }
                assertEquals(expectedFqName, ((PackageViewDescriptor) referenceTarget).getFqName());
                continue;
            }

            PsiElement actual = referenceTarget == null
                                ? bindingContext.get(BindingContext.LABEL_TARGET, referenceExpression)
                                : DescriptorToSourceUtils.descriptorToDeclaration(referenceTarget);
            if (actual instanceof KtSimpleNameExpression) {
                actual = ((KtSimpleNameExpression)actual).getIdentifier();
            }

            String actualName = null;
            if (actual != null) {
                actualName = declarationToName.get(actual);
                if (actualName == null) {
                    actualName = actual.toString();
                }
            }
            assertNotNull(element.getText(), reference);

            assertEquals(
                    "Reference `" + name + "`" + renderReferenceInContext(reference) + " is resolved into " + actualName + ".",
                    expected, actual);
        }

        for (Map.Entry<Position, String> entry : positionToType.entrySet()) {
            Position position = entry.getKey();
            String typeName = entry.getValue();

            PsiElement element = position.getElement();
            KtExpression expression = getAncestorOfType(KtExpression.class, element);

            KotlinType expressionType = bindingContext.getType(expression);
            TypeConstructor expectedTypeConstructor;
            if (typeName.startsWith(STANDARD_PREFIX)) {
                String name = typeName.substring(STANDARD_PREFIX.length());
                ClassifierDescriptor expectedClass = getBuiltinClass(name);
                expectedTypeConstructor = expectedClass.getTypeConstructor();
            }
            else {
                Position declarationPosition = declarationToPosition.get(typeName);
                assertNotNull("Undeclared: " + typeName, declarationPosition);
                PsiElement declElement = declarationPosition.getElement();
                assertNotNull(declarationPosition);
                KtDeclaration declaration = getAncestorOfType(KtDeclaration.class, declElement);
                assertNotNull(declaration);
                if (declaration instanceof KtClass) {
                    ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, declaration);
                    expectedTypeConstructor = classDescriptor.getTypeConstructor();
                }
                else if (declaration instanceof KtTypeParameter) {
                    TypeParameterDescriptor typeParameterDescriptor = bindingContext.get(BindingContext.TYPE_PARAMETER, (KtTypeParameter) declaration);
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

    @NotNull
    public static ClassifierDescriptor getBuiltinClass(String nameOrFqName) {
        ClassifierDescriptor expectedClass;

        if (nameOrFqName.indexOf('.') >= 0) {
            expectedClass = JvmPlatform.INSTANCE.getBuiltIns().getBuiltInClassByFqNameNullable(FqName.fromSegments(Arrays.asList(nameOrFqName.split("\\."))));
        }
        else {
            expectedClass = JvmPlatform.INSTANCE.getBuiltIns().getBuiltInClassByNameNullable(Name.identifier(nameOrFqName));
        }
        assertNotNull("Expected class not found: " + nameOrFqName, expectedClass);

        return expectedClass;
    }

    private static String renderReferenceInContext(KtReferenceExpression referenceExpression) {
        KtExpression statement = referenceExpression;
        while (true) {
            PsiElement parent = statement.getParent();
            if (!(parent instanceof KtExpression)) break;
            if (parent instanceof KtBlockExpression) break;
            statement = (KtExpression) parent;
        }
        KtDeclaration declaration = PsiTreeUtil.getParentOfType(referenceExpression, KtDeclaration.class);



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
        return d == null ? "<null>" : DescriptorRenderer.FQ_NAMES_IN_TYPES.render(d);
    }
}
