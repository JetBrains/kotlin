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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Nikolay Krasko
 */
public class JetCompletionContributor extends CompletionContributor {

    // A hack to avoid doubling of completion
    final HashSet<LookupPositionObject> positions = new HashSet<LookupPositionObject>();

    public JetCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                 final @NotNull CompletionResultSet result) {

                       synchronized (positions) {
                           positions.clear();

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
                                   addReferenceVariant(result, variant);
                               }
                           }

                           if (shouldRunClassNameCompletion(parameters)) {
                               addJavaClasses(parameters, result);
                               addJetClasses(result, position);
                           }

                           if (shouldRunFunctionNameCompletion(parameters)) {
                               addJetTopLevelFunctions(result, position);
                           }

                           result.stopHere();
                       }
                   }
               });
    }

    private void addReferenceVariant(@NotNull CompletionResultSet result, @NotNull Object variant) {
        if (variant instanceof LookupElement) {
            addCompletionToResult(result, (LookupElement) variant);
        }
        else {
            addCompletionToResult(result, LookupElementBuilder.create(variant.toString()));
        }
    }

    private void addJetTopLevelFunctions(@NotNull CompletionResultSet result, @NotNull PsiElement position) {
        String actualPrefix = getActualCompletionPrefix(position);
        if (actualPrefix != null) {
            final Project project = position.getProject();

            final JetShortNamesCache namesCache = JetCacheManager.getInstance(position.getProject()).getNamesCache();
            final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
            final Collection<String> functionNames = namesCache.getAllTopLevelFunctionNames();

            for (String name : functionNames) {
                if (name.contains(actualPrefix)) {
                    for (JetNamedFunction function : namesCache.getTopLevelFunctionsByName(name, scope)) {
                        String functionName = function.getName();
                        String qualifiedName = function.getQualifiedName();
                        assert functionName != null;
                        
                        final LookupElementBuilder lookup = LookupElementBuilder.create(
                                new LookupPositionObject(function), functionName);

                        if (qualifiedName != null) {
                            lookup.setTailText(qualifiedName);
                        }

                        addCompletionToResult(result, lookup);
                    }
                }
            }
        }
    }

    private void addJetClasses(@NotNull CompletionResultSet result, @NotNull PsiElement position) {
        String actualPrefix = getActualCompletionPrefix(position);
        if (actualPrefix != null) {
            final Collection<ClassDescriptor> classDescriptors =
                    JetCacheManager.getInstance(position.getProject()).getNamesCache().getClassDescriptors();

            for (ClassDescriptor descriptor : classDescriptors) {
                if (descriptor.getName().startsWith(actualPrefix)) {
                    addCompletionToResult(result, DescriptorLookupConverter.createLookupElement(null, descriptor, null));
                }
            }
        }
    }

    @Nullable
    private static String getActualCompletionPrefix(@NotNull PsiElement position) {
        if (position.getParent() instanceof JetSimpleNameExpression) {
            final JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) position.getParent();

            // Should be checked before call completion as pre-condition
            assert (PsiTreeUtil.getParentOfType(nameExpression, JetQualifiedExpression.class) == null);

            final String referencedName = nameExpression.getReferencedName();

            if (referencedName != null && referencedName.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)) {
                int lastPrefixIndex = referencedName.length() -
                                      CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.length();

                return referencedName.substring(0, lastPrefixIndex);
            }
        }

        return null;
    }

    private void addJavaClasses(@NotNull CompletionParameters parameters, @NotNull final CompletionResultSet result) {

        CompletionResultSet tempResult = result.withPrefixMatcher(CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));

        JavaClassNameCompletionContributor.addAllClasses(parameters, JavaCompletionSorting.addJavaSorting(
                parameters, tempResult), parameters.getInvocationCount() <= 1, new Consumer<LookupElement>() {

            @Override
            public void consume(@NotNull LookupElement element) {
                addCompletionToResult(result, element);
            }
        });
    }

    private static boolean shouldRunFunctionNameCompletion(@NotNull CompletionParameters parameters) {
        return shouldRunClassNameCompletion(parameters);
    }

    private static boolean shouldRunClassNameCompletion(@NotNull CompletionParameters parameters) {
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
//            if (reference == null && parent instanceof CompositeElement) {
//                reference = ((CompositeElement) parent).getPsi().getReference();
//            }
        }

        return null;
    }

    private void addCompletionToResult(@NotNull final CompletionResultSet result, LookupElement element) {
        if (element.getObject() instanceof LookupPositionObject) {
            final LookupPositionObject positionObject = (LookupPositionObject) element.getObject();

            if (positions.contains(positionObject)) {
                return;
            }
            else {
                positions.add(positionObject);
            }
        }

        result.addElement(element);
    }
}
