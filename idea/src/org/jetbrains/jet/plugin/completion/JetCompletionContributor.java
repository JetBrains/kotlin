package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

/**
 * @author Nikolay Krasko
 */
public class JetCompletionContributor extends CompletionContributor {
    public JetCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                 final @NotNull CompletionResultSet _result) {

                       if (_result.getPrefixMatcher().getPrefix().isEmpty()) {
                           return;
                       }

                       final PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       CompletionResultSet result = _result.withPrefixMatcher(CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));
                       JavaClassNameCompletionContributor.addAllClasses(parameters, JavaCompletionSorting.addJavaSorting(
                               parameters, result), parameters.getInvocationCount() <= 1, new Consumer<LookupElement>() {
                           @Override
                           public void consume(LookupElement element) {
                               _result.addElement(element);
                           }
                       });
                   }
               });
    }

//    @Override
//    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
//        final PsiFile file = context.getFile();
//
//        if (file instanceof JetFile) {
//            autoImport(file, context.getStartOffset() - 1, context.getEditor());
//        }
//    }
//
//    private static void autoImport(final PsiFile file, int offset, final Editor editor) {
//        final CharSequence text = editor.getDocument().getCharsSequence();
//        while (offset > 0 && Character.isJavaIdentifierPart(text.charAt(offset))) offset--;
//        if (offset <= 0) return;
//
//        while (offset > 0 && Character.isWhitespace(text.charAt(offset))) offset--;
//        if (offset <= 0 || text.charAt(offset) != '.') return;
//
//        offset--;
//
//        while (offset > 0 && Character.isWhitespace(text.charAt(offset))) offset--;
//        if (offset <= 0) return;
//
//        final JetSimpleNameExpression nameExpression =
//                PsiTreeUtil.findElementOfClassAtOffset(file, offset, JetSimpleNameExpression.class, false);
//        if (nameExpression == null) return;
//
//        final ImportClassFix importClassFix = new ImportClassFix(nameExpression);
//        if (importClassFix.isAvailable(file.getProject(), editor, file)) {
//            new ImportClassFix(nameExpression).invoke(file.getProject(), editor, file);
//        }
//    }
}
