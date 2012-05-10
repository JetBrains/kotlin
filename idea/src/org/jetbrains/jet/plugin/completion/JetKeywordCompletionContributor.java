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

package org.jetbrains.jet.plugin.completion;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.CommentUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.filters.*;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.psi.filters.position.LeftNeighbour;
import com.intellij.psi.filters.position.PositionElementFilter;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler;
import org.jetbrains.jet.plugin.completion.handlers.JetKeywordInsertHandler;
import org.jetbrains.jet.plugin.completion.handlers.JetTemplateInsertHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * A keyword contributor for Kotlin
 *
 * @author Nikolay Krasko
 */
public class JetKeywordCompletionContributor extends CompletionContributor {

    private final static InsertHandler<LookupElement> KEYWORDS_INSERT_HANDLER = new JetKeywordInsertHandler();
    private final static InsertHandler<LookupElement> FUNCTION_INSERT_HANDLER = new JetFunctionInsertHandler(
            JetFunctionInsertHandler.CaretPosition.AFTER_BRACKETS);

    private final static ElementFilter GENERAL_FILTER = new NotFilter(new OrFilter(
            new CommentFilter(), // or
            new ParentFilter(new ClassFilter(JetLiteralStringTemplateEntry.class)), // or
            new ParentFilter(new ClassFilter(JetConstantExpression.class)), // or
            new LeftNeighbour(new TextFilter("."))
    ));

    private final static ElementFilter NOT_IDENTIFIER_FILTER = new NotFilter(new AndFilter(
            new LeafElementFilter(JetTokens.IDENTIFIER),
            new NotFilter(new ParentFilter(new ClassFilter(JetReferenceExpression.class))))
    );

    private final static List<String> FUNCTION_KEYWORDS = Lists.newArrayList(GET_KEYWORD.toString(), SET_KEYWORD.toString());

    private static final String IF_TEMPLATE = "if (<#<condition>#>) {\n<#<block>#>\n}";
    private static final String IF_ELSE_TEMPLATE = "if (<#<condition>#>) {\n<#<block>#>\n} else {\n<#<block>#>\n}";
    private static final String IF_ELSE_ONELINE_TEMPLATE = "if (<#<condition>#>) <#<value>#> else <#<value>#>";
    private static final String FUN_TEMPLATE = "fun <#<name>#>(<#<params>#>) : <#<returnType>#> {\n<#<body>#>\n}";
    private static final String VAL_SIMPLE_TEMPLATE = "val <#<name>#> = <#<value>#>";
    private static final String VAL_WITH_TYPE_TEMPLATE = "val <#<name>#> : <#<valType>#> = <#<value>#>";
    private static final String VAL_WITH_GETTER_TEMPLATE = "val <#<name>#> : <#<valType>#>\nget() {\n<#<body>#>\n}";
    private static final String VAR_SIMPLE_TEMPLATE = "var <#<name>#> = <#<value>#>";
    private static final String VAR_WITH_TYPE_TEMPLATE = "var <#<name>#> : <#<varType>#> = <#<initial>#>";
    private static final String VAR_WITH_GETTER_AND_SETTER_TEMPLATE = "var <#<name>#> : <#<varType>#>\nget() {\n<#<body>#>\n}\nset(value) {\n<#<body>#>\n}";
    private static final String TRAIT_TEMPLATE = "trait <#<name>#> {\n<#<body>#>\n}";
    private static final String CLASS_TEMPLATE = "class <#<name>#> {\n<#<body>#>\n}";
    private static final String CLASS_OBJECT_TEMPLATE = "class object {\n<#<body>#>\n}";
    private static final String CLASS_OBJECT_WITHOUT_CLASS_TEMPLATE = "object {\n<#<body>#>\n}";
    private static final String FOR_TEMPLATE = "for (<#<i>#> in <#<elements>#>) {\n<#<body>#>\n}";
    private static final String WHEN_TEMPLATE = "when (<#<expression>#>) {\n<#<condition>#> -> <#<value>#>\n" +
                                                "else -> <#<elseValue>#>\n}";
    private static final String WHEN_ENTRY_TEMPLATE = "<#<condition>#> -> <#<value>#>";
    private static final String WHILE_TEMPLATE = "while (<#<condition>#>) {\n<#<body>#>\n}";
    private static final String DO_WHILE_TEMPLATE = "do {\n<#<body>#>\n} while (<#<condition>#>)";
    private static final String ENUM_CLASS_TEMPLATE = "enum class <#<name>#> {\n<#<body>#>\n}";

    private static class CommentFilter implements ElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            if (!(element instanceof PsiElement)) {
                return false;
            }

            return CommentUtil.isComment((PsiElement) element);
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return true;
        }
    }

    private static class ParentFilter extends PositionElementFilter {
        public ParentFilter(ElementFilter filter) {
            setFilter(filter);
        }

        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            if (!(element instanceof PsiElement)) {
                return false;
            }
            PsiElement parent = ((PsiElement) element).getParent();
            return parent != null && getFilter().isAcceptable(parent, context);
        }
    }

    private static class InTopFilter implements ElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            //noinspection unchecked
            return PsiTreeUtil.getParentOfType(context, JetFile.class, false, JetClassBody.class, JetBlockExpression.class) != null &&
                   PsiTreeUtil.getParentOfType(context, JetParameterList.class, JetTypeParameterList.class) == null;
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return true;
        }
    }

    private static class InNonClassBlockFilter implements ElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            //noinspection unchecked
            return PsiTreeUtil.getParentOfType(context, JetBlockExpression.class, true, JetClassBody.class) != null;
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return true;
        }
    }

    private static class InParametersFilter implements ElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            return PsiTreeUtil.getParentOfType(context, JetParameterList.class, false) != null;
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return true;
        }
    }

    private static class InClassBodyFilter implements ElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            //noinspection unchecked
            return PsiTreeUtil.getParentOfType(context, JetClassBody.class, true,
                                               JetBlockExpression.class, JetProperty.class, JetParameterList.class) != null;
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return true;
        }
    }

    private static class AfterClassInClassBodyFilter extends InClassBodyFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            if (super.isAcceptable(element, context)) {
                PsiElement ps = context.getPrevSibling();
                if (ps instanceof PsiWhiteSpace) {
                    ps = ps.getPrevSibling();
                }
                if (ps instanceof LeafPsiElement) {
                    return ((LeafPsiElement) ps).getElementType() == JetTokens.CLASS_KEYWORD;
                }
            }
            return false;
        }
    }

    private static class InPropertyFilter implements ElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            return PsiTreeUtil.getParentOfType(context, JetProperty.class, false) != null;
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return true;
        }
    }

    private static class InWhenFilter implements ElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            return PsiTreeUtil.getParentOfType(context, JetWhenExpression.class, false) != null;
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
            return true;
        }
    }

    public static class KeywordsCompletionProvider extends CompletionProvider<CompletionParameters> {

        private final Collection<LookupElement> elements;

        public KeywordsCompletionProvider(String... keywords) {
            List<String> elementsList = Lists.newArrayList(keywords);
            elements = Collections2.transform(elementsList, new Function<String, LookupElement>() {
                @Override
                public LookupElement apply(String keyword) {
                    final LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(keyword).setBold();

                    if (keyword.contains("<#<")) {
                        return JetTemplateInsertHandler.lookup(keyword);
                    }

                    if (!FUNCTION_KEYWORDS.contains(keyword)) {
                        return lookupElementBuilder.setInsertHandler(KEYWORDS_INSERT_HANDLER);
                    }

                    return lookupElementBuilder.setInsertHandler(FUNCTION_INSERT_HANDLER);
                }
            });
        }

        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            result.addAllElements(elements);
        }

    }

    public JetKeywordCompletionContributor() {
        registerScopeKeywordsCompletion(new InTopFilter(),
                                        ABSTRACT_KEYWORD,
                                        FINAL_KEYWORD, GET_KEYWORD,
                                        IMPORT_KEYWORD, INLINE_KEYWORD, INTERNAL_KEYWORD,
                                        OPEN_KEYWORD, PACKAGE_KEYWORD, PRIVATE_KEYWORD,
                                        PROTECTED_KEYWORD, PUBLIC_KEYWORD, SET_KEYWORD,
                                        TYPE_KEYWORD);

        registerScopeKeywordsCompletion(new InClassBodyFilter(),
                                        ABSTRACT_KEYWORD,
                                        FINAL_KEYWORD, GET_KEYWORD,
                                        INLINE_KEYWORD, INTERNAL_KEYWORD,
                                        OPEN_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
                                        PROTECTED_KEYWORD, PUBLIC_KEYWORD, SET_KEYWORD,
                                        TYPE_KEYWORD);

        registerScopeKeywordsCompletion(new InNonClassBlockFilter(),
                                        AS_KEYWORD, BREAK_KEYWORD, BY_KEYWORD,
                                        CATCH_KEYWORD, CONTINUE_KEYWORD,
                                        ELSE_KEYWORD,
                                        FALSE_KEYWORD, FINALLY_KEYWORD,
                                        GET_KEYWORD,
                                        IN_KEYWORD, INLINE_KEYWORD, INTERNAL_KEYWORD,
                                        IS_KEYWORD, NULL_KEYWORD, OBJECT_KEYWORD,
                                        PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD,
                                        RETURN_KEYWORD, SET_KEYWORD, SUPER_KEYWORD,
                                        CAPITALIZED_THIS_KEYWORD, THIS_KEYWORD, THROW_KEYWORD,
                                        TRUE_KEYWORD, TRY_KEYWORD,
                                        TYPE_KEYWORD,
                                        VARARG_KEYWORD, WHERE_KEYWORD);

        registerScopeKeywordsCompletion(new InPropertyFilter(),
                                        ELSE_KEYWORD, FALSE_KEYWORD,
                                        NULL_KEYWORD, THIS_KEYWORD, TRUE_KEYWORD);

        registerScopeKeywordsCompletion(new InParametersFilter(), OUT_KEYWORD);

        // templates
        registerScopeKeywordsCompletion(new InWhenFilter(),
                                        WHEN_ENTRY_TEMPLATE);
        registerScopeKeywordsCompletion(new InTopFilter(),
                                        FUN_TEMPLATE, VAL_WITH_TYPE_TEMPLATE, VAL_WITH_GETTER_TEMPLATE,
                                        VAR_WITH_TYPE_TEMPLATE, VAR_WITH_GETTER_AND_SETTER_TEMPLATE,
                                        TRAIT_TEMPLATE, CLASS_TEMPLATE, ENUM_CLASS_TEMPLATE);
        registerScopeKeywordsCompletion(new InClassBodyFilter(),
                                        FUN_TEMPLATE, VAL_WITH_TYPE_TEMPLATE, VAL_WITH_GETTER_TEMPLATE,
                                        VAR_WITH_TYPE_TEMPLATE, VAR_WITH_GETTER_AND_SETTER_TEMPLATE,
                                        TRAIT_TEMPLATE, CLASS_TEMPLATE, CLASS_OBJECT_TEMPLATE,
                                        ENUM_CLASS_TEMPLATE);
        registerScopeKeywordsCompletion(new InNonClassBlockFilter(),
                                        IF_TEMPLATE, IF_ELSE_TEMPLATE, IF_ELSE_ONELINE_TEMPLATE,
                                        FUN_TEMPLATE, VAL_SIMPLE_TEMPLATE, VAR_SIMPLE_TEMPLATE,
                                        TRAIT_TEMPLATE, CLASS_TEMPLATE, FOR_TEMPLATE,
                                        WHEN_TEMPLATE, WHILE_TEMPLATE, DO_WHILE_TEMPLATE, ENUM_CLASS_TEMPLATE);
        registerScopeKeywordsCompletion(new InPropertyFilter(),
                                        IF_ELSE_ONELINE_TEMPLATE, WHEN_TEMPLATE);
        registerScopeKeywordsCompletion(new AfterClassInClassBodyFilter(), false,
                                        CLASS_OBJECT_WITHOUT_CLASS_TEMPLATE);
    }

    private void registerScopeKeywordsCompletion(final ElementFilter placeFilter, boolean notIdentifier, String... keywords) {
        extend(CompletionType.BASIC, getPlacePattern(placeFilter, notIdentifier),
               new KeywordsCompletionProvider(keywords));
    }

    private void registerScopeKeywordsCompletion(final ElementFilter placeFilter, String... keywords) {
        registerScopeKeywordsCompletion(placeFilter, true, keywords);
    }

    private void registerScopeKeywordsCompletion(final ElementFilter placeFilter, JetToken... keywords) {
        registerScopeKeywordsCompletion(placeFilter, convertTokensToStrings(keywords));
    }

    private static String[] convertTokensToStrings(JetToken... keywords) {
        final ArrayList<String> strings = new ArrayList<String>(keywords.length);
        for (JetToken keyword : keywords) {
            strings.add(keyword.toString());
        }

        return ArrayUtil.toStringArray(strings);
    }

    private static ElementPattern<PsiElement> getPlacePattern(final ElementFilter placeFilter, boolean notIdentifier) {
        if (notIdentifier) {
            return PlatformPatterns.psiElement().and(
                    new FilterPattern(new AndFilter(GENERAL_FILTER, NOT_IDENTIFIER_FILTER, placeFilter)));
        }
        else {
            return PlatformPatterns.psiElement().and(
                    new FilterPattern(new AndFilter(GENERAL_FILTER, placeFilter)));
        }
    }
}
