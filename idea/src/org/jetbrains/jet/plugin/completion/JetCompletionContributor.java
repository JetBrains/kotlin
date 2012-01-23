package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetUserType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.caches.JetCacheManager;

import java.util.Collection;

/**
 * @author Nikolay Krasko
 */
public class JetCompletionContributor extends CompletionContributor {
    public JetCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                 final @NotNull CompletionResultSet result) {

                       if (result.getPrefixMatcher().getPrefix().isEmpty()) {
                           return;
                       }

                       final PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       if (shouldRunClassNameCompletion(parameters, context)) {
                           addJavaClasses(parameters, result);
                           addJetClasses(result, position);
                       }
                   }
               });
    }

    private static void addJetClasses(CompletionResultSet result, PsiElement position) {
        if (position.getParent() instanceof JetSimpleNameExpression) {
            final JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) position.getParent();
            if (PsiTreeUtil.getParentOfType(nameExpression, JetQualifiedExpression.class) == null) {
                Project project = position.getProject();

                final String referencedName = nameExpression.getReferencedName();

                if (referencedName != null && referencedName.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)) {
                    int lastPrefixIndex = referencedName.length() -
                                          CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.length();
                    String actualPrefix = referencedName.substring(0, lastPrefixIndex);

                    final Collection<ClassDescriptor> classDescriptors =
                            JetCacheManager.getInstance(project).getNamesCache().getClassDescriptors();

                    for (ClassDescriptor descriptor : classDescriptors) {
                        if (descriptor.getName().startsWith(actualPrefix)) {
                            result.addElement(DescriptorLookupConverter.createLookupElement(descriptor, null));
                        }
                    }
                }
            }
        }
    }

    private static void addJavaClasses(CompletionParameters parameters, final CompletionResultSet result) {

        CompletionResultSet tempResult = result.withPrefixMatcher(CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));

        JavaClassNameCompletionContributor.addAllClasses(parameters, JavaCompletionSorting.addJavaSorting(
                parameters, tempResult), parameters.getInvocationCount() <= 1, new Consumer<LookupElement>() {

            @Override
            public void consume(LookupElement element) {
                result.addElement(element);

            }
        });
    }

    private static boolean shouldRunClassNameCompletion(@NotNull CompletionParameters parameters, ProcessingContext context) {
        final PsiElement element = parameters.getPosition();

        if (parameters.getInvocationCount() > 1) {
            return true;
        }
        
        if (element.getNode().getElementType() == JetTokens.IDENTIFIER) {
            if (element.getParent() instanceof JetSimpleNameExpression) {
                JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) element.getParent();
                if (PsiTreeUtil.getParentOfType(nameExpression, JetQualifiedExpression.class) != null) {
                    return false;
                }

                if (PsiTreeUtil.getParentOfType(nameExpression, JetUserType.class) != null) {
                    return true;
                }

                return parameters.getInvocationCount() == 1;
            }
        }

        return false;
    }

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        super.beforeCompletion(context);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
