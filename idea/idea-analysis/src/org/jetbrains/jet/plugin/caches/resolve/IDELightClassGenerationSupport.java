/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.KotlinLightClassForExplicitDeclaration;
import org.jetbrains.jet.asJava.LightClassConstructionContext;
import org.jetbrains.jet.asJava.LightClassGenerationSupport;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.libraries.JetSourceNavigationHelper;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;
import org.jetbrains.jet.plugin.stubindex.JetAllPackagesIndex;
import org.jetbrains.jet.plugin.stubindex.JetClassByPackageIndex;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;
import org.jetbrains.jet.plugin.stubindex.PackageIndexUtil;

import java.util.*;

import static org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope.kotlinSources;

public class IDELightClassGenerationSupport extends LightClassGenerationSupport {

    private static final Logger LOG = Logger.getInstance(IDELightClassGenerationSupport.class);

    public static IDELightClassGenerationSupport getInstanceForIDE(@NotNull Project project) {
        return (IDELightClassGenerationSupport) ServiceManager.getService(project, LightClassGenerationSupport.class);
    }

    private final Project project;

    private final Comparator<JetFile> jetFileComparator;

    public IDELightClassGenerationSupport(@NotNull Project project) {
        this.project = project;
        final GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
        this.jetFileComparator = new Comparator<JetFile>() {
            @Override
            public int compare(@NotNull JetFile o1, @NotNull JetFile o2) {
                VirtualFile f1 = o1.getVirtualFile();
                VirtualFile f2 = o2.getVirtualFile();
                if (f1 == f2) return 0;
                if (f1 == null) return -1;
                if (f2 == null) return 1;
                return searchScope.compare(f1, f2);
            }
        };
    }

    @NotNull
    @Override
    public LightClassConstructionContext getContextForPackage(@NotNull Collection<JetFile> files) {
        assert !files.isEmpty() : "No files in package";

        List<JetFile> sortedFiles = new ArrayList<JetFile>(files);
        Collections.sort(sortedFiles, jetFileComparator);

        ResolveSessionForBodies session = ResolvePackage.getLazyResolveSession(sortedFiles.get(0));
        forceResolvePackageDeclarations(files, session);
        return new LightClassConstructionContext(session.getBindingContext(), session.getModuleDescriptor());
    }

    @NotNull
    @Override
    public LightClassConstructionContext getContextForClassOrObject(@NotNull JetClassOrObject classOrObject) {
        ResolveSessionForBodies session = ResolvePackage.getLazyResolveSession(classOrObject);

        if (classOrObject.isLocal()) {
            BindingContext bindingContext = session.resolveToElement(classOrObject);
            ClassDescriptor descriptor = bindingContext.get(BindingContext.CLASS, classOrObject);

            if (descriptor == null) {
                LOG.warn("No class descriptor in context for class: " + JetPsiUtil.getElementTextWithContext(classOrObject));
                return new LightClassConstructionContext(BindingContext.EMPTY, session.getModuleDescriptor());
            }

            ForceResolveUtil.forceResolveAllContents(descriptor);

            return new LightClassConstructionContext(bindingContext, session.getModuleDescriptor());
        }

        ForceResolveUtil.forceResolveAllContents(session.getClassDescriptor(classOrObject));
        return new LightClassConstructionContext(session.getBindingContext(), session.getModuleDescriptor());
    }

    private static void forceResolvePackageDeclarations(@NotNull Collection<JetFile> files, @NotNull KotlinCodeAnalyzer session) {
        for (JetFile file : files) {
            // SCRIPT: not supported
            if (file.isScript()) continue;

            FqName packageFqName = file.getPackageFqName();

            // make sure we create a package descriptor
            PackageViewDescriptor packageDescriptor = session.getModuleDescriptor().getPackage(packageFqName);
            if (packageDescriptor == null) {
                LOG.warn("No descriptor found for package " + packageFqName + " in file " + file.getName() + "\n" + file.getText());
                session.forceResolveAll();
                continue;
            }

            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetFunction) {
                    JetFunction jetFunction = (JetFunction) declaration;
                    Name name = jetFunction.getNameAsSafeName();
                    Collection<FunctionDescriptor> functions = packageDescriptor.getMemberScope().getFunctions(name);
                    for (FunctionDescriptor descriptor : functions) {
                        ForceResolveUtil.forceResolveAllContents(descriptor);
                    }
                }
                else if (declaration instanceof JetProperty) {
                    JetProperty jetProperty = (JetProperty) declaration;
                    Name name = jetProperty.getNameAsSafeName();
                    Collection<VariableDescriptor> properties = packageDescriptor.getMemberScope().getProperties(name);
                    for (VariableDescriptor descriptor : properties) {
                        ForceResolveUtil.forceResolveAllContents(descriptor);
                    }
                }
                else if (declaration instanceof JetClassOrObject) {
                    // Do nothing: we are not interested in classes
                }
                else {
                    LOG.error("Unsupported declaration kind: " + declaration + " in file " + file.getName() + "\n" + file.getText());
                }
            }
        }
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope) {
        return JetFullClassNameIndex.getInstance().get(fqName.asString(), project, kotlinSources(searchScope, project));
    }

    @NotNull
    @Override
    public Collection<JetFile> findFilesForPackage(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope) {
        return PackageIndexUtil.findFilesWithExactPackage(fqName, kotlinSources(searchScope, project), project);
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarationsInPackage(
            @NotNull FqName packageFqName, @NotNull GlobalSearchScope searchScope
    ) {
        return JetClassByPackageIndex.getInstance().get(packageFqName.asString(), project, kotlinSources(searchScope, project));
    }

    @Override
    public boolean packageExists(@NotNull FqName fqName, @NotNull GlobalSearchScope scope) {
        return !JetAllPackagesIndex.getInstance().get(fqName.asString(), project, kotlinSources(scope, project)).isEmpty();
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackages(@NotNull FqName fqn, @NotNull GlobalSearchScope scope) {
        return PackageIndexUtil.getSubPackageFqNames(fqn, kotlinSources(scope, project), project);
    }

    @Nullable
    @Override
    public PsiClass getPsiClass(@NotNull JetClassOrObject classOrObject) {
        VirtualFile virtualFile = classOrObject.getContainingFile().getVirtualFile();
        if (virtualFile != null && LibraryUtil.findLibraryEntry(virtualFile, classOrObject.getProject()) != null) {
            return JetSourceNavigationHelper.getOriginalClass(classOrObject);
        }

        return  KotlinLightClassForExplicitDeclaration.create(classOrObject.getManager(), classOrObject);
    }

    @NotNull
    public MultiMap<String, FqName> getAllPossiblePackageClasses(@NotNull GlobalSearchScope scope) {
        Collection<String> packageFqNames = JetAllPackagesIndex.getInstance().getAllKeys(project);

        MultiMap<String, FqName> result = new MultiMap<String, FqName>();
        for (String packageFqName : packageFqNames) {
            FqName packageClassFqName = PackageClassUtils.getPackageClassFqName(new FqName(packageFqName));
            result.putValue(packageClassFqName.shortName().asString(), packageClassFqName);
        }

        return result;
    }
}
