package org.jetbrains.jet.resolve;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

/**
 * @author abreslav
 */
public class ExpectedResolveData {

    private final Map<String, Integer> declarationToPosition = new HashMap<String, Integer>();
    private final Map<Integer, String> positionToReference = new HashMap<Integer, String>();
    private final Map<Integer, String> positionToType = new HashMap<Integer, String>();

    public ExpectedResolveData(final Document document) {
        new WriteCommandAction.Simple(null) {
          public void run() {
              extractData(document);
          }
        }.execute().throwException();
    }

    private void extractData(Document document) {
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
    }

    public void checkResult(JetFile file) {
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(file.getProject(), ErrorHandler.THROW_EXCEPTION);
        JetStandardLibrary lib = semanticServices.getStandardLibrary();
        Map<String, DeclarationDescriptor> nameToDescriptor = new HashMap<String, DeclarationDescriptor>();
        nameToDescriptor.put("std::Int.plus(Int)", standardFunction(lib.getInt(), "plus", lib.getIntType()));

        BindingContext bindingContext = AnalyzingUtils.analyzeFile(file, ErrorHandler.THROW_EXCEPTION);

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

            JetDeclaration expected = nameToDeclaration.get(name);

            JetReferenceExpression reference = getAncestorOfType(JetReferenceExpression.class, element);
            if (expected == null && name.startsWith("std::")) {
                DeclarationDescriptor expectedDescriptor = nameToDescriptor.get(name);
                JetTypeReference typeReference = getAncestorOfType(JetTypeReference.class, element);
                if (expectedDescriptor != null) {
                    DeclarationDescriptor actual = bindingContext.resolveReferenceExpression(reference);
                    assertSame(expectedDescriptor, actual);
                    continue;
                }

                Type actualType = bindingContext.resolveTypeReference(typeReference);
                assertNotNull("Type " + name + " not resolved for reference " + name, actualType);
                ClassDescriptor expectedClass = lib.getLibraryScope().getClass(name.substring(5));
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
            assertSame(
                    "Reference `" + name + "`" + reference.getReferencedName() + " at " + reference.getTextOffset() + " is resolved into " + actualName + ".",
                    expected, actual);
        }

        for (Map.Entry<Integer, String> entry : positionToType.entrySet()) {
            Integer position = entry.getKey();
            String typeName = entry.getValue();

            PsiElement element = file.findElementAt(position);
            JetExpression expression = getAncestorOfType(JetExpression.class, element);

            Type expressionType = bindingContext.getExpressionType(expression);
            TypeConstructor expectedTypeConstructor;
            if (typeName.startsWith("std::")) {
                ClassDescriptor expectedClass = lib.getLibraryScope().getClass(typeName.substring(5));
                assertNotNull("Expected class not found: " + typeName);
                expectedTypeConstructor = expectedClass.getTypeConstructor();
            } else {
                Integer declarationPosition = declarationToPosition.get(typeName);
                assertNotNull("Undeclared: " + typeName, declarationPosition);
                PsiElement declElement = file.findElementAt(declarationPosition);
                assertNotNull(declarationPosition);
                JetDeclaration declaration = getAncestorOfType(JetDeclaration.class, declElement);
                assertNotNull(declaration);
                ClassDescriptor classDescriptor = bindingContext.getClassDescriptor((JetClass) declaration);
                expectedTypeConstructor = classDescriptor.getTypeConstructor();
            }

            assertSame("At " + position + ": ", expectedTypeConstructor, expressionType.getConstructor());
        }
    }

    private DeclarationDescriptor standardFunction(ClassDescriptor classDescriptor, String name, Type parameterType) {
        FunctionGroup functionGroup = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList()).getFunctionGroup(name);
        Collection<FunctionDescriptor> functions = functionGroup.getPossiblyApplicableFunctions(Collections.<Type>emptyList(), Collections.singletonList(parameterType));
        for (FunctionDescriptor function : functions) {
            if (function.getUnsubstitutedValueParameters().get(0).getType().equals(parameterType)) {
                return function;
            }
        }
        throw new IllegalArgumentException("Not found: std::" + classDescriptor.getName() + "." + name + "(" + parameterType + ")");
    }

    private <T> T getAncestorOfType(Class<T> type, PsiElement element) {
        while (element != null && !type.isInstance(element)) {
            element = element.getParent();
        }
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        T result = (T) element;
        return result;
    }
}
