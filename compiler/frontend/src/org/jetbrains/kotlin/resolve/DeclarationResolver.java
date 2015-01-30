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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;
import org.jetbrains.kotlin.psi.JetPackageDirective;
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider;
import org.jetbrains.kotlin.resolve.lazy.TopLevelDescriptorProvider;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.utils.UtilsPackage;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.REDECLARATION;

public class DeclarationResolver {
    private AnnotationResolver annotationResolver;
    private BindingTrace trace;


    @Inject
    public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }


    public void resolveAnnotationsOnFiles(@NotNull TopDownAnalysisContext c, @NotNull final FileScopeProvider scopeProvider) {
        Map<JetFile, JetScope> file2scope = UtilsPackage.keysToMap(c.getFiles(), new Function1<JetFile, JetScope>() {
            @Override
            public JetScope invoke(JetFile file) {
                return scopeProvider.getFileScope(file);
            }
        });

        resolveAnnotationsOnFiles(file2scope);
    }

    private void resolveAnnotationsOnFiles(@NotNull Map<JetFile, ? extends JetScope> file2scope) {
        for (Map.Entry<JetFile, ? extends JetScope> entry : file2scope.entrySet()) {
            JetFile file = entry.getKey();
            JetScope fileScope = entry.getValue();
            annotationResolver.resolveAnnotationsWithArguments(fileScope, file.getAnnotationEntries(), trace);
            annotationResolver.resolveAnnotationsWithArguments(fileScope, file.getDanglingAnnotations(), trace);
        }
    }

    @NotNull
    public static ConstructorDescriptor getConstructorOfDataClass(@NotNull ClassDescriptor classDescriptor) {
        Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
        assert constructors.size() == 1 : "Data class must have only one constructor: " + classDescriptor.getConstructors();
        return constructors.iterator().next();
    }

    public void checkRedeclarationsInInnerClassNames(@NotNull TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes classDescriptor : c.getDeclaredClasses().values()) {
            if (classDescriptor.getKind() == ClassKind.CLASS_OBJECT) {
                // Class objects should be considered during analysing redeclarations in classes
                continue;
            }

            Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getScopeForMemberLookup().getOwnDeclaredDescriptors();

            ClassDescriptorWithResolutionScopes classObj = classDescriptor.getDefaultObjectDescriptor();
            if (classObj != null) {
                Collection<DeclarationDescriptor> classObjDescriptors = classObj.getScopeForMemberLookup().getOwnDeclaredDescriptors();
                if (!classObjDescriptors.isEmpty()) {
                    allDescriptors = Lists.newArrayList(allDescriptors);
                    allDescriptors.addAll(classObjDescriptors);
                }
            }

            Multimap<Name, DeclarationDescriptor> descriptorMap = HashMultimap.create();
            for (DeclarationDescriptor desc : allDescriptors) {
                if (desc instanceof ClassDescriptor || desc instanceof PropertyDescriptor) {
                    descriptorMap.put(desc.getName(), desc);
                }
            }

            reportRedeclarations(descriptorMap);
        }
    }

    private void reportRedeclarations(@NotNull Multimap<Name, DeclarationDescriptor> descriptorMap) {
        Set<Pair<PsiElement, Name>> redeclarations = Sets.newHashSet();
        for (Name name : descriptorMap.keySet()) {
            Collection<DeclarationDescriptor> descriptors = descriptorMap.get(name);
            if (descriptors.size() > 1) {
                // We mustn't compare PropertyDescriptor with PropertyDescriptor because we do this at OverloadResolver
                for (DeclarationDescriptor descriptor : descriptors) {
                    if (descriptor instanceof ClassDescriptor) {
                        for (DeclarationDescriptor descriptor2 : descriptors) {
                            if (descriptor == descriptor2) {
                                continue;
                            }

                            redeclarations.add(Pair.create(
                                    DescriptorToSourceUtils.classDescriptorToDeclaration((ClassDescriptor) descriptor), descriptor.getName()
                            ));
                            if (descriptor2 instanceof PropertyDescriptor) {
                                redeclarations.add(Pair.create(
                                        DescriptorToSourceUtils.descriptorToDeclaration(descriptor2), descriptor2.getName()
                                ));
                            }
                        }
                    }
                }
            }
        }
        for (Pair<PsiElement, Name> redeclaration : redeclarations) {
            trace.report(REDECLARATION.on(redeclaration.getFirst(), redeclaration.getSecond().asString()));
        }
    }

    public void checkRedeclarationsInPackages(
            @NotNull TopLevelDescriptorProvider topLevelDescriptorProvider,
            @NotNull Multimap<FqName, JetElement> topLevelFqNames
    ) {
        for (Map.Entry<FqName, Collection<JetElement>> entry : topLevelFqNames.asMap().entrySet()) {
            FqName fqName = entry.getKey();
            Collection<JetElement> declarationsOrPackageDirectives = entry.getValue();

            if (fqName.isRoot()) continue;

            Set<DeclarationDescriptor> descriptors = getTopLevelDescriptorsByFqName(topLevelDescriptorProvider, fqName);

            if (descriptors.size() > 1) {
                for (JetElement declarationOrPackageDirective : declarationsOrPackageDirectives) {
                    PsiElement reportAt = declarationOrPackageDirective instanceof JetNamedDeclaration
                                          ? declarationOrPackageDirective
                                          : ((JetPackageDirective) declarationOrPackageDirective).getNameIdentifier();
                    trace.report(Errors.REDECLARATION.on(reportAt, fqName.shortName().asString()));
                }
            }
        }
    }

    @NotNull
    private static Set<DeclarationDescriptor> getTopLevelDescriptorsByFqName(
            @NotNull TopLevelDescriptorProvider topLevelDescriptorProvider,
            @NotNull FqName fqName
    ) {
        FqName parentFqName = fqName.parent();

        Set<DeclarationDescriptor> descriptors = new HashSet<DeclarationDescriptor>();

        LazyPackageDescriptor parentFragment = topLevelDescriptorProvider.getPackageFragment(parentFqName);
        if (parentFragment != null) {
            // Filter out extension properties
            descriptors.addAll(
                    KotlinPackage.filter(
                            parentFragment.getMemberScope().getProperties(fqName.shortName()),
                            new Function1<VariableDescriptor, Boolean>() {
                                @Override
                                public Boolean invoke(VariableDescriptor descriptor) {
                                    return descriptor.getExtensionReceiverParameter() == null;
                                }
                            }
                    )
            );
        }

        ContainerUtil.addIfNotNull(descriptors, topLevelDescriptorProvider.getPackageFragment(fqName));

        descriptors.addAll(topLevelDescriptorProvider.getTopLevelClassDescriptors(fqName));
        return descriptors;
    }


}
