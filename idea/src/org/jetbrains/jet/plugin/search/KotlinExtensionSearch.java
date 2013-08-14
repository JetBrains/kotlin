package org.jetbrains.jet.plugin.search;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.*;
import com.intellij.psi.search.*;
import com.intellij.psi.search.searches.ReferenceDescriptor;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JetClsMethod;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.List;

public class KotlinExtensionSearch extends QueryFactory<PsiReference, KotlinExtensionSearch.SearchParameters> {
    public static final KotlinExtensionSearch INSTANCE = new KotlinExtensionSearch();

    public static class SearchParameters {
        private final PsiMethod method;
        private final SearchScope searchScope;
        private final SearchRequestCollector optimizer = new SearchRequestCollector(new SearchSession());

        public SearchParameters(@NotNull PsiMethod method, @NotNull SearchScope searchScope) {
            this.searchScope = searchScope;
            this.method = method;
        }

        public PsiMethod getMethod() {
            return method;
        }

        public SearchScope getSearchScope() {
            return searchScope;
        }

        public SearchRequestCollector getOptimizer() {
            return optimizer;
        }
    }

    private static class ExtensionTextOccurenceProcessor extends RequestResultProcessor {
        private final SearchParameters parameters;

        private ExtensionTextOccurenceProcessor(SearchParameters parameters, @NotNull Object... equality) {
            super(equality);
            this.parameters = parameters;
        }

        private void processElement(
                @NotNull PsiReference ref,
                @Nullable PsiElement refTarget,
                @NotNull Processor<PsiReference> consumer
        ) {
            if (!(refTarget instanceof JetSimpleNameExpression)) return;

            BindingContext bindingContext =
                    AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) refTarget.getContainingFile()).getBindingContext();
            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) refTarget);

            if (!(descriptor instanceof FunctionDescriptor)) return;

            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            ReceiverParameterDescriptor receiverParameterDescriptor = functionDescriptor.getReceiverParameter();

            if (receiverParameterDescriptor == null) return;
            JetType receiverType = receiverParameterDescriptor.getType();

            ClassifierDescriptor receiverDescriptor = receiverType.getConstructor().getDeclarationDescriptor();
            if (!(receiverDescriptor instanceof ClassDescriptor)) return;

            PsiElement originalMethod = parameters.getMethod();
            if (originalMethod instanceof JetClsMethod) {
                originalMethod = ((JetClsMethod) originalMethod).getOrigin();
            }

            DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, originalMethod);
            if (declarationDescriptor == null) return;

            DeclarationDescriptor containingDescriptor = declarationDescriptor.getContainingDeclaration();
            if (!(containingDescriptor instanceof ClassDescriptor)) return;

            if (DescriptorUtils.isSubclass((ClassDescriptor) containingDescriptor, (ClassDescriptor) receiverDescriptor)) {
                consumer.process(ref);
            }
        }

        @Override
        public boolean processTextOccurrence(
                @NotNull PsiElement element,
                int offsetInElement,
                @NotNull Processor<PsiReference> consumer
        ) {
            List<PsiReference> references = PsiReferenceService.getService().getReferences(
                    element, PsiReferenceService.Hints.NO_HINTS
            );
            for (PsiReference ref : references) {
                if (ReferenceRange.containsOffsetInElement(ref, offsetInElement)) {
                    processElement(ref, ref.getElement(), consumer);
                }
            }

            return true;
        }
    }

    private KotlinExtensionSearch() {
        registerExecutor(
                new QueryExecutorBase<PsiReference, SearchParameters>() {
                    @Override
                    public void processQuery(@NotNull final SearchParameters p, @NotNull Processor<PsiReference> consumer) {
                        ApplicationManager.getApplication().runReadAction(new Runnable() {
                            @Override
                            public void run() {
                                p.getOptimizer().searchWord(
                                        p.getMethod().getName(),
                                        p.getSearchScope(),
                                        UsageSearchContext.IN_CODE,
                                        true,
                                        new ExtensionTextOccurenceProcessor(p)
                                );
                            }
                        });
                    }
                }
        );
    }

    public static Query<PsiReference> search(@NotNull PsiMethod method, @NotNull SearchScope scope) {
        return search(new SearchParameters(method, scope));
    }

    public static Query<PsiReference> search(SearchParameters parameters) {
        Query<PsiReference> result = INSTANCE.createQuery(parameters);
        SearchRequestCollector requests = parameters.getOptimizer();
        return uniqueResults(new MergeQuery<PsiReference>(result, new SearchRequestQuery(parameters.getMethod().getProject(), requests)));
    }

    private static UniqueResultsQuery<PsiReference, ReferenceDescriptor> uniqueResults(@NotNull Query<PsiReference> composite) {
        return new UniqueResultsQuery<PsiReference, ReferenceDescriptor>(
                composite, ContainerUtil.<ReferenceDescriptor>canonicalStrategy(), ReferenceDescriptor.MAPPER
        );
    }
}
