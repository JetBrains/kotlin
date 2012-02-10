package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetUserType;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.completion.handlers.JetJavaClassInsertHandler;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

import java.util.Collection;
import java.util.HashSet;

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

                       final HashSet<LookupPositionObject> positions = new HashSet<LookupPositionObject>();

                       if (result.getPrefixMatcher().getPrefix().isEmpty()) {
                           return;
                       }

                       final PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       final JetSimpleNameReference jetReference = getJetReference(parameters);
                       if (jetReference != null) {
                           for (Object variant : jetReference.getVariants()) {
                               addReferenceVariant(result, variant, positions, parameters.getPosition().getProject());
                           }
                       }

                       if (shouldRunTopLevelCompletion(parameters)) {
                           addClasses(parameters, result, positions);
                           addJetTopLevelFunctions(result, position, positions);
                       }

                       result.stopHere();
                   }
               });
    }

    private static void addReferenceVariant(
            @NotNull CompletionResultSet result, 
            @NotNull Object variant,
            @NotNull final HashSet<LookupPositionObject> positions,
            @NotNull final Project project) {

        if (variant instanceof LookupElement) {
            addCompletionToResult(result, (LookupElement) variant, positions, project);
        }
        else {
            addCompletionToResult(result, LookupElementBuilder.create(variant.toString()), positions, project);
        }
    }

    private static void addJetTopLevelFunctions(@NotNull CompletionResultSet result, @NotNull PsiElement position,
                                                @NotNull final HashSet<LookupPositionObject> positions) {
        
        String actualPrefix = result.getPrefixMatcher().getPrefix();

        final Project project = position.getProject();

        final JetShortNamesCache namesCache = JetCacheManager.getInstance(position.getProject()).getNamesCache();
        final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        final Collection<String> functionNames = namesCache.getAllTopLevelFunctionNames();

        BindingContext resolutionContext = namesCache.getResolutionContext(scope);

        for (String name : functionNames) {
            if (name.contains(actualPrefix)) {
                for (FunctionDescriptor function : namesCache.getTopLevelFunctionDescriptorsByName(name, scope)) {
                    addCompletionToResult(result, DescriptorLookupConverter.createLookupElement(resolutionContext, function), positions, project);
                }
            }
        }
    }

    /**
     * Jet classes will be added as java completions for unification
     */
    private static void addClasses(
            @NotNull final CompletionParameters parameters,
            @NotNull final CompletionResultSet result,
            @NotNull final HashSet<LookupPositionObject> positions) {

        CompletionResultSet tempResult = result.withPrefixMatcher(CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));

        JavaClassNameCompletionContributor.addAllClasses(
                parameters,
                parameters.getInvocationCount() <= 2,
                JavaCompletionSorting.addJavaSorting(parameters, tempResult).getPrefixMatcher(),
                new Consumer<LookupElement>() {
                    @Override
                    public void consume(@NotNull LookupElement element) {
                        // Redefine standard java insert handler which is going to insert fqn
                        if (element instanceof JavaPsiClassReferenceElement) {
                            JavaPsiClassReferenceElement javaPsiReferenceElement = (JavaPsiClassReferenceElement) element;
                            javaPsiReferenceElement.setInsertHandler(JetJavaClassInsertHandler.JAVA_CLASS_INSERT_HANDLER);
                        }

                        addCompletionToResult(result, element, positions, parameters.getPosition().getProject());
                    }
                });
    }

    private static boolean shouldRunTopLevelCompletion(@NotNull CompletionParameters parameters) {
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
                    return parameters.getInvocationCount() == 1;
                }
            }
        }

        return false;
    }

    @Nullable
    private static JetSimpleNameReference getJetReference(@NotNull CompletionParameters parameters) {
        final PsiElement element = parameters.getPosition();
        if (element.getParent() != null) {
            final PsiElement parent = element.getParent();
            PsiReference[] references = parent.getReferences();

            if (references.length != 0) {
                for (PsiReference reference : references) {
                    if (reference instanceof JetSimpleNameReference) {
                        return (JetSimpleNameReference) reference;
                    }
                }
            }
        }

        return null;
    }

    private static void addCompletionToResult(
            @NotNull final CompletionResultSet result,
            @NotNull LookupElement element,
            @NotNull HashSet<LookupPositionObject> positions,
            @NotNull Project project) {

        final LookupPositionObject lookupPosition = getLookupPosition(element, project);
        if (lookupPosition != null && !positions.contains(lookupPosition)) {
            positions.add(lookupPosition);
            result.addElement(element);    
        }
    }

    private static LookupPositionObject getLookupPosition(LookupElement element, Project project) {
        final Object lookupObject = element.getObject();
        if (lookupObject instanceof PsiElement) {
            return new LookupPositionObject((PsiElement) lookupObject);
        }
        else if (lookupObject instanceof JetLookupObject) {
            final PsiElement psiElement = ((JetLookupObject) lookupObject).getPsiElement();
            if (psiElement != null) {
                return new LookupPositionObject(psiElement);
            }
        }

        return null;
    }
}
