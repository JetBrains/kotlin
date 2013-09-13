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

package org.jetbrains.jet.plugin.completion;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.filters.*;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.psi.filters.position.LeftNeighbour;
import com.intellij.psi.filters.position.PositionElementFilter;
import com.intellij.psi.filters.position.SuperParentFilter;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.completion.handlers.JetKeywordInsertHandler;
import org.jetbrains.jet.plugin.completion.weigher.WeigherPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.*;
import static org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler.EMPTY_FUNCTION_HANDLER;

/**
 * A keyword contributor for Kotlin
 */
public class JetKeywordCompletionContributor extends CompletionContributor {
    private final static InsertHandler<LookupElement> KEYWORDS_INSERT_HANDLER = new JetKeywordInsertHandler();

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

    private static class CommentFilter implements ElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            if (!(element instanceof PsiElement)) {
                return false;
            }

            return JetPsiUtil.isInComment((PsiElement) element);
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

    private static class InTopFilter extends PositionElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            //noinspection unchecked
            return PsiTreeUtil.getParentOfType(context, JetFile.class, false, JetClass.class, JetClassBody.class, JetBlockExpression.class,
                                               JetFunction.class) != null &&
                   PsiTreeUtil.getParentOfType(context, JetParameterList.class, JetTypeParameterList.class, JetClass.class) == null;
        }
    }

    private static class InNonClassBlockFilter extends PositionElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            //noinspection unchecked
            return PsiTreeUtil.getParentOfType(context, JetBlockExpression.class, true, JetClassBody.class) != null;
        }
    }

    private static class InTypeParameterFirstChildFilter extends PositionElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            JetTypeParameter typeParameterElement = PsiTreeUtil.getParentOfType(context, JetTypeParameter.class, true);
            if (typeParameterElement != null && PsiTreeUtil.isAncestor(typeParameterElement.getFirstChild(), context, false)) {
                return true;
            }

            return false;
        }
    }

    private static class InClassBodyFilter extends PositionElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            //noinspection unchecked
            return PsiTreeUtil.getParentOfType(context, JetClassBody.class, true,
                                               JetBlockExpression.class, JetProperty.class, JetParameterList.class) != null;
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

    private static class InPropertyBodyFilter extends PositionElementFilter {
        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            if (!(element instanceof PsiElement)) return false;
            JetProperty property = PsiTreeUtil.getParentOfType(context, JetProperty.class, false);
            return property != null && isAfterName(property, (PsiElement) element);
        }

        private static boolean isAfterName(@NotNull JetProperty property, @NotNull PsiElement element) {
            for (PsiElement child = property.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (PsiTreeUtil.isAncestor(child, element, false)) {
                    break;
                }

                if (child.getNode().getElementType() == IDENTIFIER) {
                    return true;
                }
            }

            return false;
        }
    }

    private static class SimplePrefixMatcher extends PrefixMatcher {
        protected SimplePrefixMatcher(String prefix) {
            super(prefix);
        }

        @Override
        public boolean prefixMatches(@NotNull String name) {
            return StringUtil.startsWith(name, getPrefix());
        }

        @NotNull
        @Override
        public PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
            return new SimplePrefixMatcher(prefix);
        }
    }

    public static class KeywordsCompletionProvider extends CompletionProvider<CompletionParameters> {

        private final Collection<LookupElement> elements;
        private final String debugName;

        public KeywordsCompletionProvider(String debugName, String... keywords) {
            this.debugName = debugName;

            List<String> elementsList = Lists.newArrayList(keywords);
            elements = Collections2.transform(elementsList, new Function<String, LookupElement>() {
                @Override
                public LookupElement apply(String keyword) {
                    LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(keyword).bold();

                    if (!FUNCTION_KEYWORDS.contains(keyword)) {
                        return lookupElementBuilder.withInsertHandler(KEYWORDS_INSERT_HANDLER);
                    }

                    return lookupElementBuilder.withInsertHandler(EMPTY_FUNCTION_HANDLER);
                }
            });
        }

        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            //noinspection StaticMethodReferencedViaSubclass
            WeigherPackage.addJetSorting(result, parameters)
                    .withPrefixMatcher(new SimplePrefixMatcher(result.getPrefixMatcher().getPrefix()))
                    .addAllElements(elements);
        }

        @Override
        public String toString() {
            return debugName;
        }
    }

    public JetKeywordCompletionContributor() {
        registerScopeKeywordsCompletion(InTopFilter.class.getName(), new InTopFilter(),
                                        ABSTRACT_KEYWORD, CLASS_KEYWORD, ENUM_KEYWORD,
                                        FINAL_KEYWORD, FUN_KEYWORD, GET_KEYWORD,
                                        IMPORT_KEYWORD, INLINE_KEYWORD, INTERNAL_KEYWORD,
                                        OPEN_KEYWORD, PACKAGE_KEYWORD, PRIVATE_KEYWORD,
                                        PROTECTED_KEYWORD, PUBLIC_KEYWORD, SET_KEYWORD, TRAIT_KEYWORD,
                                        TYPE_KEYWORD, VAL_KEYWORD, VAR_KEYWORD);

        registerScopeKeywordsCompletion(
                "In modifier list but not in parameters",
                new AndFilter(
                        new SuperParentFilter(new ClassFilter(JetModifierList.class)),
                        new NotFilter(new InTypeParameterFirstChildFilter())),
                ABSTRACT_KEYWORD, FINAL_KEYWORD, INLINE_KEYWORD, INTERNAL_KEYWORD,
                OPEN_KEYWORD, PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD);

        registerScopeKeywordsCompletion(InClassBodyFilter.class.getName(), new InClassBodyFilter(),
                                        ABSTRACT_KEYWORD, CLASS_KEYWORD, ENUM_KEYWORD,
                                        FINAL_KEYWORD, FUN_KEYWORD, GET_KEYWORD,
                                        INLINE_KEYWORD, INTERNAL_KEYWORD,
                                        OPEN_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
                                        PROTECTED_KEYWORD, PUBLIC_KEYWORD, SET_KEYWORD, TRAIT_KEYWORD,
                                        TYPE_KEYWORD, VAL_KEYWORD, VAR_KEYWORD);

        registerScopeKeywordsCompletion(InNonClassBlockFilter.class.getName(), new InNonClassBlockFilter(),
                                        AS_KEYWORD, BREAK_KEYWORD, BY_KEYWORD,
                                        CATCH_KEYWORD, CLASS_KEYWORD, CONTINUE_KEYWORD,
                                        DO_KEYWORD, ELSE_KEYWORD, ENUM_KEYWORD,
                                        FALSE_KEYWORD, FINALLY_KEYWORD, FOR_KEYWORD, FUN_KEYWORD,
                                        GET_KEYWORD, IF_KEYWORD,
                                        IN_KEYWORD, INLINE_KEYWORD, INTERNAL_KEYWORD,
                                        IS_KEYWORD, NULL_KEYWORD, OBJECT_KEYWORD,
                                        PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD,
                                        RETURN_KEYWORD, SET_KEYWORD, SUPER_KEYWORD,
                                        CAPITALIZED_THIS_KEYWORD, THIS_KEYWORD, THROW_KEYWORD,
                                        TRAIT_KEYWORD, TRUE_KEYWORD, TRY_KEYWORD,
                                        TYPE_KEYWORD, VAL_KEYWORD, VAR_KEYWORD,
                                        VARARG_KEYWORD, WHEN_KEYWORD, WHERE_KEYWORD, WHILE_KEYWORD);

        registerScopeKeywordsCompletion(InPropertyBodyFilter.class.getName(), new InPropertyBodyFilter(),
                                        ELSE_KEYWORD, FALSE_KEYWORD, IF_KEYWORD,
                                        NULL_KEYWORD, THIS_KEYWORD, TRUE_KEYWORD, WHEN_KEYWORD);

        registerScopeKeywordsCompletion(InTypeParameterFirstChildFilter.class.getName(), new InTypeParameterFirstChildFilter(), false,
                                        IN_KEYWORD, OUT_KEYWORD);

        registerScopeKeywordsCompletion(AfterClassInClassBodyFilter.class.getName(), new AfterClassInClassBodyFilter(), false,
                                        OBJECT_KEYWORD);
    }

    private void registerScopeKeywordsCompletion(String debugName, ElementFilter placeFilter, boolean notIdentifier, String... keywords) {
        extend(CompletionType.BASIC, getPlacePattern(placeFilter, notIdentifier), new KeywordsCompletionProvider(debugName, keywords));
    }

    private void registerScopeKeywordsCompletion(String debugName, ElementFilter placeFilter, boolean notIdentifier, JetToken... keywords) {
        registerScopeKeywordsCompletion(debugName, placeFilter, notIdentifier, convertTokensToStrings(keywords));
    }

    private void registerScopeKeywordsCompletion(String debugName, ElementFilter placeFilter, JetToken... keywords) {
        registerScopeKeywordsCompletion(debugName, placeFilter, true, convertTokensToStrings(keywords));
    }

    private static String[] convertTokensToStrings(JetToken... keywords) {
        ArrayList<String> strings = new ArrayList<String>(keywords.length);
        for (JetToken keyword : keywords) {
            strings.add(keyword.toString());
        }

        return ArrayUtil.toStringArray(strings);
    }

    private static ElementPattern<PsiElement> getPlacePattern(ElementFilter placeFilter, boolean notIdentifier) {
        if (notIdentifier) {
            return PlatformPatterns.psiElement().and(new FilterPattern(new AndFilter(GENERAL_FILTER, NOT_IDENTIFIER_FILTER, placeFilter)));
        }
        else {
            return PlatformPatterns.psiElement().and(new FilterPattern(new AndFilter(GENERAL_FILTER, placeFilter)));
        }
    }
}
