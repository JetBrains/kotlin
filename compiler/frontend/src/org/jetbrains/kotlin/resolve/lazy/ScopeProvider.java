/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy;

import com.intellij.psi.PsiElement;
import kotlin.Function0;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetImportDirective;
import org.jetbrains.kotlin.psi.JetImportsFactory;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;
import org.jetbrains.kotlin.storage.NotNullLazyValue;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ScopeProvider implements DeclarationScopeProvider, FileScopeProvider {

    public static class AdditionalFileScopeProvider {
        @NotNull
        public List<JetScope> scopes(@NotNull JetFile file) {
            return Collections.emptyList();
        }
    }

    private final ResolveSession resolveSession;

    private final NotNullLazyValue<Collection<JetImportDirective>> defaultImports;

    private final MemoizedFunctionToNotNull<JetFile, LazyFileScope> fileScopes;

    private AdditionalFileScopeProvider additionalFileScopeProvider;
    private DeclarationScopeProvider declarationScopeProvider;

    @Inject
    public void setAdditionalFileScopesProvider(@NotNull AdditionalFileScopeProvider additionalFileScopeProvider) {
        this.additionalFileScopeProvider = additionalFileScopeProvider;
    }

    @Inject
    public void setDeclarationScopeProvider(@NotNull DeclarationScopeProviderImpl declarationScopeProvider) {
        this.declarationScopeProvider = declarationScopeProvider;
    }

    public ScopeProvider(@NotNull final ResolveSession resolveSession) {
        this.resolveSession = resolveSession;

        this.defaultImports = resolveSession.getStorageManager().createLazyValue(new Function0<Collection<JetImportDirective>>() {
            @Override
            public Collection<JetImportDirective> invoke() {
                PackageViewDescriptor rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT);
                if (rootPackage == null) {
                    throw new IllegalStateException("Root package not found");
                }

                JetImportsFactory importsFactory = resolveSession.getJetImportsFactory();
                List<ImportPath> defaultImports = resolveSession.getModuleDescriptor().getDefaultImports();

                return importsFactory.createImportDirectives(defaultImports);
            }
        });


        this.fileScopes = resolveSession.getStorageManager().createMemoizedFunction(new Function1<JetFile, LazyFileScope>() {
            @Override
            public LazyFileScope invoke(JetFile file) {
                return createFileScope(file);
            }
        });
    }

    @Override
    @NotNull
    public LazyFileScope getFileScope(@NotNull JetFile file) {
        return fileScopes.invoke(file);
    }

    private LazyFileScope createFileScope(@NotNull JetFile file) {
        TemporaryBindingTrace tempTrace = TemporaryBindingTrace.create(resolveSession.getTrace(), "Transient trace for default imports lazy resolve");
        return LazyFileScope.OBJECT$.create(
                resolveSession,
                file,
                defaultImports.invoke(),
                additionalFileScopeProvider.scopes(file),
                resolveSession.getTrace(),
                tempTrace,
                "LazyFileScope for file " + file.getName());
    }

    @NotNull
    @Override
    public JetScope getResolutionScopeForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        return declarationScopeProvider.getResolutionScopeForDeclaration(elementOfDeclaration);
    }

    @NotNull
    @Override
    public DataFlowInfo getOuterDataFlowInfoForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        return declarationScopeProvider.getOuterDataFlowInfoForDeclaration(elementOfDeclaration);
    }
}
