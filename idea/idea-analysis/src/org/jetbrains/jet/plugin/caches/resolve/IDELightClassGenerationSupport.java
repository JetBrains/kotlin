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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import kotlin.Function1;
import kotlin.KotlinPackage;
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
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.libraries.JetSourceNavigationHelper;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;
import org.jetbrains.jet.plugin.stubindex.JetClassByPackageIndex;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;
import org.jetbrains.jet.plugin.stubindex.PackageIndexUtil;

import java.util.*;

import static org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope.kotlinSources;

public class IDELightClassGenerationSupport extends LightClassGenerationSupport {

    private static final Logger LOG = Logger.getInstance(IDELightClassGenerationSupport.class);

    private final Project project;

    private final Comparator<JetFile> jetFileComparator;

    public IDELightClassGenerationSupport(@NotNull Project project) {
        this.project = project;
        this.jetFileComparator = byScopeComparator(GlobalSearchScope.allScope(project));
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
                return new LightClassConstructionContext(bindingContext, session.getModuleDescriptor());
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
    public List<KotlinLightPackageClassInfo> findPackageClassesInfos(
            @NotNull FqName fqName, @NotNull GlobalSearchScope wholeScope
    ) {
        Collection<JetFile> allFiles = findFilesForPackage(fqName, wholeScope);
        Map<IdeaModuleInfo, List<JetFile>> filesByInfo = groupByModuleInfo(allFiles);
        List<KotlinLightPackageClassInfo> result = new ArrayList<KotlinLightPackageClassInfo>();
        for (Map.Entry<IdeaModuleInfo, List<JetFile>> entry : filesByInfo.entrySet()) {
            result.add(new KotlinLightPackageClassInfo(entry.getValue(), entry.getKey().contentScope()));
        }
        sortByClasspath(wholeScope, result);
        return result;
    }

    @NotNull
    private static Map<IdeaModuleInfo, List<JetFile>> groupByModuleInfo(@NotNull Collection<JetFile> allFiles) {
        return KotlinPackage.groupByTo(
                allFiles,
                new LinkedHashMap<IdeaModuleInfo, List<JetFile>>(),
                new Function1<JetFile, IdeaModuleInfo>() {
                    @Override
                    public IdeaModuleInfo invoke(JetFile file) {
                        return ResolvePackage.getModuleInfo(file);
                    }
                });
    }

    private static void sortByClasspath(@NotNull GlobalSearchScope wholeScope, @NotNull List<KotlinLightPackageClassInfo> result) {
        final Comparator<JetFile> byScopeComparator = byScopeComparator(wholeScope);
        Collections.sort(result, new Comparator<KotlinLightPackageClassInfo>() {
            @Override
            public int compare(@NotNull KotlinLightPackageClassInfo info1, @NotNull KotlinLightPackageClassInfo info2) {
                JetFile file1 = info1.getFiles().iterator().next();
                JetFile file2 = info2.getFiles().iterator().next();
                //classes earlier that would appear earlier on classpath should go first
                return -byScopeComparator.compare(file1, file2);
            }
        });
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
        return PackageIndexUtil.packageExists(fqName, kotlinSources(scope, project), project);
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
    private static Comparator<JetFile> byScopeComparator(@NotNull final GlobalSearchScope searchScope) {
        return new Comparator<JetFile>() {
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
}
