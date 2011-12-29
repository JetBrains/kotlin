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
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler;
import org.jetbrains.jet.plugin.completion.handlers.JetKeywordInsertHandler;

import java.util.Collection;
import java.util.List;

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

    private final static List<String> FUNCTION_KEYWORDS = Lists.newArrayList("get", "set");

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
            return PsiTreeUtil.getParentOfType(context, JetFile.class, false, JetClass.class, JetFunction.class) != null;
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
            return PsiTreeUtil.getParentOfType(context, JetClassBody.class, true, JetBlockExpression.class) != null;
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
                                        "abstract", "class", "enum", "final", "fun", "get", "import", "inline",
                                        "internal", "open", "package", "private", "protected", "public", "set",
                                        "trait", "type", "val", "var");

        registerScopeKeywordsCompletion(new InClassBodyFilter(),
                                        "abstract", "class", "enum", "final", "fun", "get", "inline", "internal",
                                        "object", "open", "override", "private", "protected", "public", "set", "trait",
                                        "type", "val", "var");

        registerScopeKeywordsCompletion(new InNonClassBlockFilter(),
                                        "as", "break", "by", "catch", "class", "continue", "default", "do", "else",
                                        "enum", "false", "finally", "for", "fun", "get", "if", "in", "inline",
                                        "internal", "is", "null", "object", "private", "protected", "public", "ref",
                                        "return", "set", "super", "This", "this", "throw", "trait", "true", "try",
                                        "type", "val", "var", "vararg", "when", "where", "while");

        registerScopeKeywordsCompletion(new InPropertyFilter(), "else", "false", "if", "null", "this", "true");

        registerScopeKeywordsCompletion(new InParametersFilter(), "ref", "out");
    }

    private void registerScopeKeywordsCompletion(final ElementFilter placeFilter, String... keywords) {
        extend(CompletionType.BASIC, getPlacePattern(placeFilter), new KeywordsCompletionProvider(keywords));
    }

    private static ElementPattern<PsiElement> getPlacePattern(final ElementFilter placeFilter) {
        return PlatformPatterns.psiElement().and(
                new FilterPattern(new AndFilter(GENERAL_FILTER, placeFilter)));
    }
}
