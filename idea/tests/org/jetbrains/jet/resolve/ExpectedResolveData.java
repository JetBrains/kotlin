package org.jetbrains.jet.resolve;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.*;

/**
 * @author abreslav
 */
public class ExpectedResolveData {

    private final Map<String, Integer> declarationToPosition = new HashMap<String, Integer>();
    private final Map<Integer, String> positionToReference = new HashMap<Integer, String>();
    private final Map<Integer, String> positionToType = new HashMap<Integer, String>();
    private final Map<String, DeclarationDescriptor> nameToDescriptor;
    private final Map<String, PsiElement> nameToPsiElement;
//    private final Map<String, JetType> nameToType;

    public ExpectedResolveData(Map<String, DeclarationDescriptor> nameToDescriptor, Map<String, PsiElement> nameToPsiElement/*, Map<String, JetType> nameToType*/) {
        this.nameToDescriptor = nameToDescriptor;
        this.nameToPsiElement = nameToPsiElement;
//        this.nameToType = nameToType;
    }

    public void extractData(final Document document) {
        new WriteCommandAction.Simple(null) {
          public void run() {
              doExtractData(document);
          }
        }.execute().throwException();
    }

    private void doExtractData(Document document) {
        String text = document.getText();

        Pattern pattern = Pattern.compile("(~[^~]+~)|(`[^`]+`)");
        while (true) {
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) break;

            String group = matcher.group();
            String name = group.substring(1, group.length() - 1);
            int start = matcher.start();
            if (group.startsWith("~")) {
                if (declarationToPosition.put(name, start) != null) {
                    throw new IllegalArgumentException("Redeclaration: " + name);
                }
            }
            else if (group.startsWith("`")) {
                if (name.startsWith(":")) {
                    positionToType.put(start - 1, name.substring(1));
                }
                else {
                    positionToReference.put(start, name);
                }
            }
            else {
                throw new IllegalStateException();
            }

            document.replaceString(start, matcher.end(), "");
            text = document.getText();
        }

//        System.out.println("text = " + text);
    }

    public void checkResult(JetFile file) {
        final Set<PsiElement> unresolvedReferences = new HashSet<PsiElement>();
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(file.getProject());
        JetStandardLibrary lib = semanticServices.getStandardLibrary();

        BindingContext bindingContext = AnalyzingUtils.analyzeNamespace(file.getRootNamespace(), JetControlFlowDataTraceFactory.EMPTY);
        AnalyzingUtils.applyHandler(new ErrorHandler() {
                    @Override
                    public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
                        unresolvedReferences.add(referenceExpression);
                    }
                }, bindingContext);

        Map<String, JetDeclaration> nameToDeclaration = new HashMap<String, JetDeclaration>();

        Map<JetDeclaration, String> declarationToName = new HashMap<JetDeclaration, String>();
        for (Map.Entry<String, Integer> entry : declarationToPosition.entrySet()) {
            String name = entry.getKey();
            Integer position = entry.getValue();
            PsiElement element = file.findElementAt(position);

            JetDeclaration ancestorOfType = getAncestorOfType(JetDeclaration.class, element);
            nameToDeclaration.put(name, ancestorOfType);
            declarationToName.put(ancestorOfType, name);
        }

        for (Map.Entry<Integer, String> entry : positionToReference.entrySet()) {
            Integer position = entry.getKey();
            String name = entry.getValue();
            PsiElement element = file.findElementAt(position);

            if ("!".equals(name)) {
                JetReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(element, JetReferenceExpression.class);
                assertTrue(
                        "Must have been unresolved: " +
                        renderReferenceInContext(referenceExpression) +
                        " but was resolved to " + DescriptorUtil.renderPresentableText(bindingContext.resolveReferenceExpression(referenceExpression)),
                        unresolvedReferences.contains(referenceExpression));
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
            if (expected == null && name.startsWith("std::")) {
                DeclarationDescriptor expectedDescriptor = nameToDescriptor.get(name);
                JetTypeReference typeReference = getAncestorOfType(JetTypeReference.class, element);
                if (expectedDescriptor != null) {
                    DeclarationDescriptor actual = bindingContext.resolveReferenceExpression(reference);
                    assertSame("Expected: " + name,  expectedDescriptor.getOriginal(), actual == null ? null : actual.getOriginal());
                    continue;
                }

                JetType actualType = bindingContext.resolveTypeReference(typeReference);
                assertNotNull("Type " + name + " not resolved for reference " + name, actualType);
                ClassifierDescriptor expectedClass = lib.getLibraryScope().getClassifier(name.substring(5));
                assertNotNull("Expected class not found: " + name);
                assertSame("Type resolution mismatch: ", expectedClass.getTypeConstructor(), actualType.getConstructor());
                continue;
            }
            assert expected != null : "No declaration for " + name;

            PsiElement actual = bindingContext.resolveToDeclarationPsiElement(reference);

            String actualName = null;
            if (actual != null) {
                actualName = declarationToName.get(actual);
                if (actualName == null) {
                    actualName = actual.toString();
                }
            }
            assertNotNull(reference);

            if (expected instanceof JetParameter || actual instanceof JetParameter) {
                DeclarationDescriptor expectedDescriptor;
                if (name.startsWith("$")) {
                    expectedDescriptor = bindingContext.getPropertyDescriptor((JetParameter) expected);
                }
                else {
                    expectedDescriptor = bindingContext.getDeclarationDescriptor(expected);
                    if (expectedDescriptor == null) {
                        expectedDescriptor = bindingContext.getConstructorDescriptor((JetElement) expected);
                    }
                }


                DeclarationDescriptor actualDescriptor = bindingContext.resolveReferenceExpression(reference);

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

        for (Map.Entry<Integer, String> entry : positionToType.entrySet()) {
            Integer position = entry.getKey();
            String typeName = entry.getValue();

            PsiElement element = file.findElementAt(position);
            JetExpression expression = getAncestorOfType(JetExpression.class, element);

            JetType expressionType = bindingContext.getExpressionType(expression);
            TypeConstructor expectedTypeConstructor;
            if (typeName.startsWith("std::")) {
                ClassifierDescriptor expectedClass = lib.getLibraryScope().getClassifier(typeName.substring(5));

                assertNotNull("Expected class not found: " + typeName, expectedClass);
                expectedTypeConstructor = expectedClass.getTypeConstructor();
            } else {
                Integer declarationPosition = declarationToPosition.get(typeName);
                assertNotNull("Undeclared: " + typeName, declarationPosition);
                PsiElement declElement = file.findElementAt(declarationPosition);
                assertNotNull(declarationPosition);
                JetDeclaration declaration = getAncestorOfType(JetDeclaration.class, declElement);
                assertNotNull(declaration);
                if (declaration instanceof JetClass) {
                    ClassDescriptor classDescriptor = bindingContext.getClassDescriptor((JetClass) declaration);
                    expectedTypeConstructor = classDescriptor.getTypeConstructor();
                }
                else if (declaration instanceof JetTypeParameter) {
                    TypeParameterDescriptor typeParameterDescriptor = bindingContext.getTypeParameterDescriptor((JetTypeParameter) declaration);
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
        return referenceExpression.getText() +
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
}
