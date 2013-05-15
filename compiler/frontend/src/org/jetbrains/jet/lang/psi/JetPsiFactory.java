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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.List;

public class JetPsiFactory {

    public static ASTNode createValNode(Project project) {
        JetProperty property = createProperty(project, "val x = 1");
        return property.getValOrVarNode();
    }

    public static ASTNode createVarNode(Project project) {
        JetProperty property = createProperty(project, "var x = 1");
        return property.getValOrVarNode();
    }

    public static ASTNode createValOrVarNode(Project project, String text) {
        return createParameterList(project, "(" + text + " int x)").getParameters().get(0).getValOrVarNode();
    }

    public static JetExpression createExpression(Project project, String text) {
        JetProperty property = createProperty(project, "val x = " + text);
        return property.getInitializer();
    }

    public static JetValueArgumentList createCallArguments(Project project, String text) {
        JetProperty property = createProperty(project, "val x = foo" + text);
        JetExpression initializer = property.getInitializer();
        JetCallExpression callExpression = (JetCallExpression) initializer;
        return callExpression.getValueArgumentList();
    }

    public static JetTypeReference createType(Project project, String type) {
        JetProperty property = createProperty(project, "val x : " + type);
        return property.getTypeRef();
    }

    @NotNull
    public static PsiElement createStar(Project project) {
        PsiElement star = createType(project, "List<*>").findElementAt(5);
        assert star != null;
        return star;
    }

    //the pair contains the first and the last elements of a range
    public static Pair<PsiElement, PsiElement> createColonAndWhiteSpaces(Project project) {
        JetProperty property = createProperty(project, "val x : Int");
        return Pair.create(property.findElementAt(5), property.findElementAt(7));
    }

    //the pair contains the first and the last elements of a range
    public static Pair<PsiElement, PsiElement> createTypeWhiteSpaceAndColon(Project project, String type) {
        JetProperty property = createProperty(project, "val x: " + type);
        return Pair.create(property.findElementAt(5), (PsiElement) property.getTypeRef());
    }

    public static ASTNode createColonNode(Project project) {
        JetProperty property = createProperty(project, "val x: Int");
        return property.getNode().findChildByType(JetTokens.COLON);
    }

    @NotNull
    public static PsiElement createSemicolon(Project project) {
        JetProperty property = createProperty(project, "val x: Int;");
        PsiElement semicolon = property.findElementAt(10);
        assert semicolon != null;
        return semicolon;
    }

    public static PsiElement createWhiteSpace(Project project) {
        return createWhiteSpace(project, " ");
    }

    private static PsiElement createWhiteSpace(Project project, String text) {
        JetProperty property = createProperty(project, "val" + text + "x");
        return property.findElementAt(3);
    }

    public static PsiElement createNewLine(Project project) {
        return createWhiteSpace(project, "\n");
    }

    public static JetClass createClass(Project project, String text) {
        return createDeclaration(project, text, JetClass.class);
    }

    @NotNull
    public static JetFile createFile(Project project, String text) {
        return createFile(project, "dummy.jet", text);
    }

    @NotNull
    public static JetFile createFile(Project project, String fileName, String text) {
        return (JetFile) PsiFileFactory.getInstance(project).createFileFromText(fileName, JetFileType.INSTANCE, text,
                                                                                LocalTimeCounter.currentTime(), false);
    }

    @NotNull
    public static JetFile createPhysicalFile(Project project, String fileName, String text) {
        return (JetFile) PsiFileFactory.getInstance(project).createFileFromText(fileName, JetFileType.INSTANCE, text,
                                                                                LocalTimeCounter.currentTime(), true);
    }

    public static JetProperty createProperty(Project project, String name, String type, boolean isVar, @Nullable String initializer) {
        String text = (isVar ? "var " : "val ") + name + (type != null ? ":" + type : "") + (initializer == null ? "" : " = " + initializer);
        return createProperty(project, text);
    }

    public static JetProperty createProperty(Project project, String name, String type, boolean isVar) {
        return createProperty(project, name, type, isVar, null);
    }

    public static JetProperty createProperty(Project project, String text) {
        return createDeclaration(project, text, JetProperty.class);
    }

    private static <T> T createDeclaration(Project project, String text, Class<T> clazz) {
        JetFile file = createFile(project, text);
        List<JetDeclaration> dcls = file.getDeclarations();
        assert dcls.size() == 1 : dcls.size() + " declarations in " + text;
        @SuppressWarnings("unchecked")
        T result = (T) dcls.get(0);
        return result;
    }

    public static PsiElement createNameIdentifier(Project project, String name) {
        return createProperty(project, name, null, false).getNameIdentifier();
    }

    public static JetSimpleNameExpression createSimpleName(Project project, String name) {
        return (JetSimpleNameExpression) createProperty(project, name, null, false, name).getInitializer();
    }

    public static PsiElement createIdentifier(Project project, String name) {
        return createSimpleName(project, name).getIdentifier();
    }

    public static JetNamedFunction createFunction(Project project, String funDecl) {
        return createDeclaration(project, funDecl, JetNamedFunction.class);
    }

    public static JetModifierList createModifierList(Project project, JetKeywordToken modifier) {
        String text = modifier.getValue() + " val x";
        JetProperty property = createProperty(project, text);
        return property.getModifierList();
    }

    public static JetModifierList createConstructorModifierList(Project project, JetKeywordToken modifier) {
        JetClass aClass = createClass(project, "class C " + modifier.getValue() + " (){}");
        return aClass.getPrimaryConstructorModifierList();
    }

    public static JetExpression createEmptyBody(Project project) {
        JetNamedFunction function = createFunction(project, "fun foo() {}");
        return function.getBodyExpression();
    }

    public static JetClassBody createEmptyClassBody(Project project) {
        JetClass aClass = createClass(project, "class A(){}");
        return aClass.getBody();
    }

    public static JetParameter createParameter(Project project, String name, String type) {
        JetNamedFunction function = createFunction(project, "fun foo(" + name + " : " + type + ") {}");
        return function.getValueParameters().get(0);
    }

    public static JetParameterList createParameterList(Project project, String text) {
        JetNamedFunction function = createFunction(project, "fun foo" + text + "{}");
        return function.getValueParameterList();
    }

    @NotNull
    public static JetWhenEntry createWhenEntry(@NotNull Project project, @NotNull String entryText) {
        JetNamedFunction function = createFunction(project, "fun foo() { when(12) { " + entryText + " } }");
        JetWhenEntry whenEntry = PsiTreeUtil.findChildOfType(function, JetWhenEntry.class);

        assert whenEntry != null : "Couldn't generate when entry";
        assert entryText.equals(whenEntry.getText()) : "Generate when entry text differs from the given text";

        return whenEntry;
    }

    @NotNull
    public static JetImportDirective createImportDirective(Project project, @NotNull String path) {
        return createImportDirective(project, new ImportPath(path));
    }

    @NotNull
    public static JetImportDirective createImportDirective(Project project, @NotNull ImportPath importPath) {
        if (importPath.fqnPart().isRoot()) {
            throw new IllegalArgumentException("import path must not be empty");
        }

        StringBuilder importDirectiveBuilder = new StringBuilder("import ");
        importDirectiveBuilder.append(importPath.getPathStr());

        Name alias = importPath.getAlias();
        if (alias != null) {
            importDirectiveBuilder.append(" as ").append(alias.getName());
        }

        JetFile namespace = createFile(project, importDirectiveBuilder.toString());
        return namespace.getImportDirectives().iterator().next();
    }

    public static PsiElement createPrimaryConstructor(Project project) {
        JetClass aClass = createClass(project, "class A()");
        return aClass.findElementAt(7).getParent();
    }

    public static JetSimpleNameExpression createClassLabel(Project project, @NotNull String labelName) {
        JetThisExpression expression = (JetThisExpression) createExpression(project, "this@" + labelName);
        return expression.getTargetLabel();
    }

    public static JetExpression createFieldIdentifier(Project project, @NotNull String fieldName) {
        return createExpression(project, "$" + fieldName);
    }

    @NotNull
    public static JetBinaryExpression createBinaryExpression(Project project, @NotNull String lhs, @NotNull String op, @NotNull String rhs) {
        return (JetBinaryExpression) createExpression(project, lhs + " " + op + " " + rhs);
    }

    @NotNull
    public static JetBinaryExpression createBinaryExpression(Project project, @Nullable JetExpression lhs, @NotNull String op, @Nullable JetExpression rhs) {
        return createBinaryExpression(project, lhs != null ? lhs.getText() : "", op, rhs != null ? rhs.getText() : "");
    }

    public static JetTypeCodeFragment createTypeCodeFragment(Project project, String text, PsiElement context) {
        return new JetTypeCodeFragmentImpl(project, "fragment.kt", text, context);
    }

    public static JetExpressionCodeFragment createExpressionCodeFragment(Project project, String text, PsiElement context) {
        return new JetExpressionCodeFragmentImpl(project, "fragment.kt", text, context);
    }

    @NotNull
    public static JetReturnExpression createReturn(Project project, @NotNull String text) {
        return (JetReturnExpression) createExpression(project, "return " + text);
    }

    @NotNull
    public static JetReturnExpression createReturn(Project project, @Nullable JetExpression expression) {
        return createReturn(project, expression != null ? expression.getText() : "");
    }

    @NotNull
    public static JetIfExpression createIf(Project project,
            @Nullable JetExpression condition, @Nullable JetExpression thenExpr, @Nullable JetExpression elseExpr) {
        return (JetIfExpression) createExpression(project, JetPsiUnparsingUtils.toIf(condition, thenExpr, elseExpr));
    }

    public static class IfChainBuilder {
        private final StringBuilder sb = new StringBuilder();
        private boolean first = true;
        private boolean frozen = false;

        public IfChainBuilder() {
        }

        @NotNull
        public IfChainBuilder ifBranch(@NotNull String conditionText, @NotNull String expressionText) {
            if (first) {
                first = false;
            } else {
                sb.append("else ");
            }

            sb.append("if (").append(conditionText).append(") ").append(expressionText).append("\n");
            return this;
        }

        @NotNull
        public IfChainBuilder ifBranch(@NotNull JetExpression condition, @NotNull JetExpression expression) {
            return ifBranch(condition.getText(), expression.getText());
        }

        @NotNull
        public IfChainBuilder elseBranch(@NotNull String expressionText) {
            sb.append("else ").append(expressionText);
            return this;
        }

        @NotNull
        public IfChainBuilder elseBranch(@Nullable JetExpression expression) {
            return elseBranch(expression != null ? expression.getText() : "");
        }

        @NotNull
        public JetIfExpression toExpression(Project project) {
            if (!frozen) {
                frozen = true;
            }
            return (JetIfExpression) createExpression(project, sb.toString());
        }
    }

    public static class WhenBuilder {
        private final StringBuilder sb = new StringBuilder("when ");
        private boolean frozen = false;
        private boolean inCondition = false;

        public WhenBuilder() {
            this((String)null);
        }

        public WhenBuilder(@Nullable String subjectText) {
            if (subjectText != null) {
                sb.append("(").append(subjectText).append(") ");
            }
            sb.append("{\n");
        }

        public WhenBuilder(@Nullable JetExpression subject) {
            this(subject != null ? subject.getText() : null);
        }

        @NotNull
        public WhenBuilder condition(@NotNull String text) {
            assert !frozen;

            if (!inCondition) {
                inCondition = true;
            } else {
                sb.append(", ");
            }
            sb.append(text);

            return this;
        }

        @NotNull
        public WhenBuilder condition(@Nullable JetExpression expression) {
            return condition(expression != null ? expression.getText() : "");
        }

        @NotNull
        public WhenBuilder pattern(@NotNull String typeReferenceText, boolean negated) {
            return condition((negated ? "!is" : "is") + " " + typeReferenceText);
        }

        @NotNull
        public WhenBuilder pattern(@Nullable JetTypeReference typeReference, boolean negated) {
            return pattern(typeReference != null ? typeReference.getText() : "", negated);
        }

        @NotNull
        public WhenBuilder range(@NotNull String argumentText, boolean negated) {
            return condition((negated ? "!in" : "in") + " " + argumentText);
        }

        @NotNull
        public WhenBuilder range(@Nullable JetExpression argument, boolean negated) {
            return range(argument != null ? argument.getText() : "", negated);
        }

        @NotNull
        public WhenBuilder branchExpression(@NotNull String expressionText) {
            assert !frozen;
            assert inCondition;

            inCondition = false;
            sb.append(" -> ").append(expressionText).append("\n");

            return this;
        }

        @NotNull
        public WhenBuilder branchExpression(@Nullable JetExpression expression) {
            return branchExpression(expression != null ? expression.getText() : "");
        }

        @NotNull
        public WhenBuilder entry(@NotNull String entryText) {
            assert !frozen;
            assert !inCondition;

            sb.append(entryText).append("\n");

            return this;
        }

        @NotNull
        public WhenBuilder entry(@Nullable JetWhenEntry whenEntry) {
            return entry(whenEntry != null ? whenEntry.getText() : "");
        }

        @NotNull
        public WhenBuilder elseEntry(@NotNull String text) {
            return entry("else -> " + text);
        }

        @NotNull
        public WhenBuilder elseEntry(@Nullable JetExpression expression) {
            return elseEntry(expression != null ? expression.getText() : "");
        }

        @NotNull
        public JetWhenExpression toExpression(Project project) {
            if (!frozen) {
                sb.append("}");
                frozen = true;
            }
            return (JetWhenExpression) createExpression(project, sb.toString());
        }
    }
}
