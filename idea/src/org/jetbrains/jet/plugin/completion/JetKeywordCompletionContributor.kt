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
import com.google.common.base.Functions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
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
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler;
import org.jetbrains.jet.plugin.completion.handlers.JetKeywordInsertHandler;
import org.jetbrains.jet.plugin.completion.weigher.WeigherPackage;

import java.util.*;
import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.*;

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

    public JetKeywordCompletionContributor() {
        ElementFilter inTopLevel = notIdentifier(new InTopFilter());
        ElementFilter inTypeParameterFirstChildFilter = new InTypeParameterFirstChildFilter();
        ElementFilter inClassBody = notIdentifier(new InClassBodyFilter());
        ElementFilter inNonClassBlock = notIdentifier(new InNonClassBlockFilter());
        ElementFilter inAfterClassInClassBody = new AfterClassInClassBodyFilter();
        ElementFilter inPropertyBody = notIdentifier(new InPropertyBodyFilter());
        ElementFilter inNonParameterModifier = notIdentifier(new AndFilter(
                new SuperParentFilter(new ClassFilter(JetModifierList.class)),
                new NotFilter(inTypeParameterFirstChildFilter)));

        BunchKeywordRegister register = new BunchKeywordRegister();

        register.add(ABSTRACT_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody);
        register.add(FINAL_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody);
        register.add(OPEN_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody);

        register.add(INTERNAL_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody, inNonClassBlock);
        register.add(PRIVATE_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody, inNonClassBlock);
        register.add(PROTECTED_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody, inNonClassBlock);
        register.add(PUBLIC_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody, inNonClassBlock);

        register.add(CLASS_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);
        register.add(ENUM_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);
        register.add(FUN_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);
        register.add(GET_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);
        register.add(SET_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);
        register.add(TRAIT_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);
        register.add(VAL_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);
        register.add(VAR_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);
        register.add(TYPE_KEYWORD, inTopLevel, inClassBody, inNonClassBlock);

        register.add(IMPORT_KEYWORD, inTopLevel);
        register.add(PACKAGE_KEYWORD, inTopLevel);

        register.add(OVERRIDE_KEYWORD, inClassBody);

        register.add(IN_KEYWORD, inNonClassBlock, inTypeParameterFirstChildFilter);
        register.add(OUT_KEYWORD, inTypeParameterFirstChildFilter);
        register.add(OBJECT_KEYWORD, inNonClassBlock, inAfterClassInClassBody);

        register.add(ELSE_KEYWORD, inNonClassBlock, inPropertyBody);
        register.add(IF_KEYWORD, inNonClassBlock, inPropertyBody);
        register.add(TRUE_KEYWORD, inNonClassBlock, inPropertyBody);
        register.add(FALSE_KEYWORD, inNonClassBlock, inPropertyBody);
        register.add(NULL_KEYWORD, inNonClassBlock, inPropertyBody);
        register.add(THIS_KEYWORD, inNonClassBlock, inPropertyBody);
        register.add(WHEN_KEYWORD, inNonClassBlock, inPropertyBody);

        register.add(AS_KEYWORD, inNonClassBlock);
        register.add(BREAK_KEYWORD, inNonClassBlock);
        register.add(BY_KEYWORD, inNonClassBlock);
        register.add(CATCH_KEYWORD, inNonClassBlock);
        register.add(CONTINUE_KEYWORD, inNonClassBlock);
        register.add(DO_KEYWORD, inNonClassBlock);
        register.add(FINALLY_KEYWORD, inNonClassBlock);
        register.add(FOR_KEYWORD, inNonClassBlock);
        register.add(IS_KEYWORD, inNonClassBlock);
        register.add(RETURN_KEYWORD, inNonClassBlock);
        register.add(SUPER_KEYWORD, inNonClassBlock);
        register.add(CAPITALIZED_THIS_KEYWORD, inNonClassBlock);
        register.add(THROW_KEYWORD, inNonClassBlock);
        register.add(TRY_KEYWORD, inNonClassBlock);
        register.add(VARARG_KEYWORD, inNonClassBlock);
        register.add(WHERE_KEYWORD, inNonClassBlock);
        register.add(WHILE_KEYWORD, inNonClassBlock);

        register.registerAll();
    }

    private static ElementFilter notIdentifier(ElementFilter filter) {
        return new AndFilter(NOT_IDENTIFIER_FILTER, filter);
    }

    private static ElementPattern<PsiElement> getPlacePattern(ElementFilter placeFilter) {
        return PlatformPatterns.psiElement().and(new FilterPattern(new AndFilter(GENERAL_FILTER, placeFilter)));
    }

    private void registerScopeKeywordsCompletion(ElementFilter placeFilter, Collection<JetToken> keywords) {
        extend(CompletionType.BASIC, getPlacePattern(placeFilter),
               new KeywordsCompletionProvider(Collections2.transform(keywords, Functions.toStringFunction())));
    }

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

        @Override
        public boolean isAcceptable(Object element, PsiElement context) {
            if (!(element instanceof PsiElement)) return false;
            JetProperty property = PsiTreeUtil.getParentOfType(context, JetProperty.class, false);
            return property != null && isAfterName(property, (PsiElement) element);
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

    private static class KeywordsCompletionProvider extends CompletionProvider<CompletionParameters> {
        private final Collection<LookupElement> elements;
        private final String debugName;

        public KeywordsCompletionProvider(Collection<String> keywords) {
            debugName = StringUtil.join(Ordering.natural().sortedCopy(keywords), ", ");
            elements = Collections2.transform(keywords, new Function<String, LookupElement>() {
                @Override
                public LookupElement apply(String keyword) {
                    LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(keyword).bold();

                    if (!FUNCTION_KEYWORDS.contains(keyword)) {
                        return lookupElementBuilder.withInsertHandler(KEYWORDS_INSERT_HANDLER);
                    }

                    return lookupElementBuilder.withInsertHandler(JetFunctionInsertHandler.EMPTY_FUNCTION_HANDLER);
                }
            });
        }

        @Override
        protected void addCompletions(
                @NotNull CompletionParameters parameters, ProcessingContext context,
                @NotNull CompletionResultSet result
        ) {
            WeigherPackage.addJetSorting(result, parameters)
                    .withPrefixMatcher(new SimplePrefixMatcher(result.getPrefixMatcher().getPrefix()))
                    .addAllElements(elements);
        }

        @Override
        public String toString() {
            return debugName;
        }
    }

    private class BunchKeywordRegister {
        private final MultiMap<HashSet<ElementFilter>, JetToken> orFiltersToKeywords = MultiMap.create();

        void add(JetToken keyword, ElementFilter... filters) {
            HashSet<ElementFilter> filtersSet = Sets.newHashSet(filters);
            orFiltersToKeywords.putValue(filtersSet, keyword);
        }

        void registerAll() {
            for (Map.Entry<HashSet<ElementFilter>, Collection<JetToken>> entry : orFiltersToKeywords.entrySet()) {
                ElementFilter[] filters = ArrayUtil.toObjectArray(entry.getKey(), ElementFilter.class);
                Collection<JetToken> tokens = entry.getValue();
                registerScopeKeywordsCompletion(new OrFilter(filters), tokens);
            }
        }
    }
}
