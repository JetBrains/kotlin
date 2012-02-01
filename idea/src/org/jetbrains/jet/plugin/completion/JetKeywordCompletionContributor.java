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
import com.intellij.psi.filters.*;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.psi.filters.position.LeftNeighbour;
import com.intellij.psi.filters.position.PositionElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;
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

    private final static List<String> FUNCTION_KEYWORDS = Lists.newArrayList(GET_KEYWORD.toString(), SET_KEYWORD.toString());

    private static final String IF_TEMPLATE = "if (<#<condition>#>) {\n<#<block>#>\n}";
    private static final String IF_ELSE_TEMPLATE = "if (<#<condition>#>) {\n<#<block>#>\n} else {\n<#<block>#>\n}";
    private static final String IF_ELSE_ONELINE_TEMPLATE = "if (<#<condition>#>) <#<value>#> else <#<value>#>";
    private static final String FUN_TEMPLATE = "fun <#<name>#>(<#<params>#>) : <#<returnType>#> {\n<#<body>#>\n}";

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

    public static class KeywordsCompletionProvider extends CompletionProvider<CompletionParameters> {

        private final Collection<LookupElement> elements;

        public KeywordsCompletionProvider(String ...keywords) {
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
                ABSTRACT_KEYWORD, CLASS_KEYWORD, ENUM_KEYWORD,
                FINAL_KEYWORD, GET_KEYWORD,
                IMPORT_KEYWORD, INLINE_KEYWORD, INTERNAL_KEYWORD,
                OPEN_KEYWORD, PACKAGE_KEYWORD, PRIVATE_KEYWORD,
                PROTECTED_KEYWORD, PUBLIC_KEYWORD, SET_KEYWORD,
                TRAIT_KEYWORD, TYPE_KEYWORD, VAL_KEYWORD,
                VAR_KEYWORD);

        registerScopeKeywordsCompletion(new InClassBodyFilter(),
                ABSTRACT_KEYWORD, CLASS_KEYWORD, ENUM_KEYWORD,
                FINAL_KEYWORD, GET_KEYWORD,
                INLINE_KEYWORD, INTERNAL_KEYWORD, OBJECT_KEYWORD,
                OPEN_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
                PROTECTED_KEYWORD, PUBLIC_KEYWORD, SET_KEYWORD,
                TRAIT_KEYWORD, TYPE_KEYWORD, VAL_KEYWORD,
                VAR_KEYWORD);

        registerScopeKeywordsCompletion(new InNonClassBlockFilter(),
                AS_KEYWORD, BREAK_KEYWORD, BY_KEYWORD,
                CATCH_KEYWORD, CLASS_KEYWORD, CONTINUE_KEYWORD,
                DO_KEYWORD, ELSE_KEYWORD, ENUM_KEYWORD,
                FALSE_KEYWORD, FINALLY_KEYWORD, FOR_KEYWORD,
                GET_KEYWORD,
                IN_KEYWORD, INLINE_KEYWORD, INTERNAL_KEYWORD,
                IS_KEYWORD, NULL_KEYWORD, OBJECT_KEYWORD,
                PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD,
                RETURN_KEYWORD, SET_KEYWORD, SUPER_KEYWORD,
                CAPITALIZED_THIS_KEYWORD, THIS_KEYWORD, THROW_KEYWORD,
                TRAIT_KEYWORD, TRUE_KEYWORD, TRY_KEYWORD,
                TYPE_KEYWORD, VAL_KEYWORD, VAR_KEYWORD,
                VARARG_KEYWORD, WHEN_KEYWORD, WHERE_KEYWORD,
                WHILE_KEYWORD);

        registerScopeKeywordsCompletion(new InPropertyFilter(),
                ELSE_KEYWORD, FALSE_KEYWORD,
                NULL_KEYWORD, THIS_KEYWORD, TRUE_KEYWORD);

        registerScopeKeywordsCompletion(new InParametersFilter(), OUT_KEYWORD);

        // templates
        registerScopeKeywordsCompletion(new InTopFilter(), FUN_TEMPLATE);
        registerScopeKeywordsCompletion(new InClassBodyFilter(), FUN_TEMPLATE);
        registerScopeKeywordsCompletion(new InNonClassBlockFilter(), IF_TEMPLATE, IF_ELSE_TEMPLATE, IF_ELSE_ONELINE_TEMPLATE, FUN_TEMPLATE);
        registerScopeKeywordsCompletion(new InPropertyFilter(), IF_ELSE_ONELINE_TEMPLATE);
        registerScopeKeywordsCompletion(new InParametersFilter(), ArrayUtil.EMPTY_STRING_ARRAY);
    }

    private void registerScopeKeywordsCompletion(final ElementFilter placeFilter, String... keywords) {
        extend(CompletionType.BASIC, getPlacePattern(placeFilter),
               new KeywordsCompletionProvider(keywords));
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

    private static ElementPattern<PsiElement> getPlacePattern(final ElementFilter placeFilter) {
        return PlatformPatterns.psiElement().and(
                new FilterPattern(new AndFilter(GENERAL_FILTER, placeFilter)));
    }

}
