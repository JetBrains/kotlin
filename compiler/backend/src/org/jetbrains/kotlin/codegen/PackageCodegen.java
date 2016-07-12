/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.context.PackageContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils;
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PackageCodegen {
    private final GenerationState state;
    private final Collection<KtFile> files;
    private final PackageFragmentDescriptor packageFragment;
    private final PackagePartRegistry packagePartRegistry;

    public PackageCodegen(
            @NotNull GenerationState state,
            @NotNull Collection<KtFile> files,
            @NotNull FqName packageFqName,
            @NotNull PackagePartRegistry registry
    ) {
        this.state = state;
        this.files = files;
        this.packageFragment = getOnlyPackageFragment(packageFqName);
        packagePartRegistry = registry;
    }

    public void generate(@NotNull CompilationErrorHandler errorHandler) {
        for (KtFile file : files) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
            try {
                generateFile(file);
                state.afterIndependentPart();
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                VirtualFile vFile = file.getVirtualFile();
                errorHandler.reportException(e, vFile == null ? "no file" : vFile.getUrl());
                DiagnosticUtils.throwIfRunningOnServer(e);
                if (ApplicationManager.getApplication().isInternal()) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            }
        }
    }

    private void generateClassesAndObjectsInFile(@NotNull List<KtClassOrObject> classOrObjects, @NotNull PackageContext packagePartContext) {
        for (KtClassOrObject classOrObject : CodegenUtilKt.sortTopLevelClassesAndPrepareContextForSealedClasses(classOrObjects, packagePartContext, state)) {
            generateClassOrObject(classOrObject, packagePartContext);
        }
    }

    private void generateFile(@NotNull KtFile file) {
        JvmFileClassInfo fileClassInfo = state.getFileClassesProvider().getFileClassInfo(file);

        if (fileClassInfo.getWithJvmMultifileClass()) {
            return;
        }

        Type fileClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(fileClassInfo.getFileClassFqName());
        PackageContext packagePartContext = state.getRootContext().intoPackagePart(packageFragment, fileClassType, file);

        boolean generatePackagePart = false;

        List<KtClassOrObject> classOrObjects = new ArrayList<KtClassOrObject>();

        for (KtDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction || declaration instanceof KtTypeAlias) {
                generatePackagePart = true;
            }
            else if (declaration instanceof KtClassOrObject) {
                KtClassOrObject classOrObject = (KtClassOrObject) declaration;
                if (state.getGenerateDeclaredClassFilter().shouldGenerateClass(classOrObject)) {
                    classOrObjects.add(classOrObject);
                }
            }
            else if (declaration instanceof KtScript) {
                KtScript script = (KtScript) declaration;

                if (state.getGenerateDeclaredClassFilter().shouldGenerateScript(script)) {
                    ScriptCodegen.createScriptCodegen(script, state, packagePartContext).generate();
                }
            }
        }
        generateClassesAndObjectsInFile(classOrObjects, packagePartContext);

        if (!generatePackagePart || !state.getGenerateDeclaredClassFilter().shouldGeneratePackagePart(file)) return;

        String name = fileClassType.getInternalName();
        packagePartRegistry.addPart(name.substring(name.lastIndexOf('/') + 1));

        ClassBuilder builder = state.getFactory().newVisitor(JvmDeclarationOriginKt.PackagePart(file, packageFragment), fileClassType, file);

        new PackagePartCodegen(builder, file, fileClassType, packagePartContext, state).generate();
    }

    @Nullable
    private PackageFragmentDescriptor getOnlyPackageFragment(@NotNull FqName expectedPackageFqName) {
        SmartList<PackageFragmentDescriptor> fragments = new SmartList<PackageFragmentDescriptor>();
        for (KtFile file : files) {
            PackageFragmentDescriptor fragment = state.getBindingContext().get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file);
            assert fragment != null : "package fragment is null for " + file + "\n" + file.getText();

            assert expectedPackageFqName.equals(fragment.getFqName()) :
                    "expected package fq name: " + expectedPackageFqName + ", actual: " + fragment.getFqName();

            if (!fragments.contains(fragment)) {
                fragments.add(fragment);
            }
        }
        if (fragments.size() > 1) {
            throw new IllegalStateException("More than one package fragment, files: " + files + " | fragments: " + fragments);
        }

        if (fragments.isEmpty()) {
            return null;
        }
        return fragments.get(0);
    }

    public void generateClassOrObject(@NotNull KtClassOrObject classOrObject, @NotNull PackageContext packagePartContext) {
        MemberCodegen.genClassOrObject(packagePartContext, classOrObject, state, null);
    }

    public Collection<KtFile> getFiles() {
        return files;
    }

    public PackageFragmentDescriptor getPackageFragment() {
        return packageFragment;
    }
}
