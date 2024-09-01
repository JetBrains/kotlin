/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.context.PackageContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.lazy.descriptors.PackageDescriptorUtilKt;
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil;
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PackageCodegenImpl implements PackageCodegen {
    private static final Logger LOG = Logger.getInstance(PackageCodegenImpl.class);

    private final GenerationState state;
    private final Collection<KtFile> files;
    private final PackageFragmentDescriptor packageFragment;

    public PackageCodegenImpl(
            @NotNull GenerationState state,
            @NotNull Collection<KtFile> files,
            @NotNull FqName packageFqName
    ) {
        this.state = state;
        this.files = files;
        this.packageFragment = getOnlyPackageFragment(packageFqName);
    }

    @Override
    public void generate() {
        for (KtFile file : files) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
            try {
                generateFile(file);
                state.afterIndependentPart();
            }
            catch (Throwable e) {
                VirtualFile vFile = file.getVirtualFile();
                CodegenUtil.reportBackendException(
                        e, "file facade code generation", vFile == null ? null : vFile.getUrl(), null, (it) -> null
                );
            }
        }
    }

    public static void generateClassesAndObjectsInFile(
            @NotNull KtFile file,
            @NotNull CodegenContext<?> context,
            @NotNull GenerationState state
    ) {
        List<KtClassOrObject> classOrObjects = new ArrayList<>();

        for (KtDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof KtClassOrObject) {
                if (PsiUtilsKt.hasExpectModifier(declaration)) {
                    addDescriptorToOptionalAnnotationsIfNeeded((KtClassOrObject) declaration, state);
                    continue;
                }

                KtClassOrObject classOrObject = (KtClassOrObject) declaration;
                if (state.getGenerateDeclaredClassFilter().shouldGenerateClass(classOrObject)) {
                    classOrObjects.add(classOrObject);
                }
            }
            else if (declaration instanceof KtScript) {
                KtScript script = (KtScript) declaration;

                if (state.getGenerateDeclaredClassFilter().shouldGenerateScript(script)) {
                    ScriptCodegen.createScriptCodegen(script, state, context).generate();
                }
            }
        }

        List<KtClassOrObject> sortedClasses =
                CodegenUtilKt.sortTopLevelClassesAndPrepareContextForSealedClasses(classOrObjects, context, state);
        for (KtClassOrObject classOrObject : sortedClasses) {
            MemberCodegen.genClassOrObject(context, classOrObject, state, null);
        }
    }

    private static void addDescriptorToOptionalAnnotationsIfNeeded(@NotNull KtClassOrObject declaration, @NotNull GenerationState state) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, declaration);
        if (descriptor == null || !OptionalAnnotationUtil.shouldGenerateExpectClass(descriptor)) {
            return;
        }

        assert OptionalAnnotationUtil.isOptionalAnnotationClass(descriptor) :
                "Expect class should be generated only if it's an optional annotation: " + descriptor;

        state.getFactory().getPackagePartRegistry().getOptionalAnnotations().add(descriptor);

        KtClassBody body = declaration.getBody();

        if (body != null) {
            for (KtDeclaration childDeclaration : body.getDeclarations()) {
                if (!(childDeclaration instanceof KtClassOrObject)) continue;
                addDescriptorToOptionalAnnotationsIfNeeded((KtClassOrObject) childDeclaration, state);
            }
        }
    }

    private void generateFile(@NotNull KtFile file) {
        JvmFileClassInfo fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file);
        if (fileClassInfo.getWithJvmMultifileClass()) return;

        Type fileClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(fileClassInfo.getFileClassFqName());
        PackageContext packagePartContext = state.getRootContext().intoPackagePart(packageFragment, fileClassType, file);

        if (file instanceof KtCodeFragment) {
            // Avoid generating light classes for code fragments
            if (state.getClassBuilderMode().generateBodies
                && state.getGenerateDeclaredClassFilter().shouldGenerateCodeFragment((KtCodeFragment) file)
            ) {
                CodeFragmentCodegen.createCodegen((KtCodeFragment) file, state, packagePartContext).generate();
            }
        } else {
            generateClassesAndObjectsInFile(file, packagePartContext, state);
        }

        if (!state.getGenerateDeclaredClassFilter().shouldGeneratePackagePart(file)) return;

        if (CodegenUtil.getMemberDeclarationsToGenerate(file).isEmpty()) return;

        state.getFactory().getPackagePartRegistry().addPart(packageFragment.getFqName(), fileClassType.getInternalName(), null);

        ClassBuilder builder = state.getFactory().newVisitor(JvmDeclarationOriginKt.PackagePart(file, packageFragment), fileClassType, file);

        new PackagePartCodegen(builder, file, fileClassType, packagePartContext, state).generate();
    }

    @Nullable
    private PackageFragmentDescriptor getOnlyPackageFragment(@NotNull FqName expectedPackageFqName) {
        SmartList<PackageFragmentDescriptor> fragments = new SmartList<>();
        for (KtFile file : files) {
            PackageFragmentDescriptor fragment =
                    PackageDescriptorUtilKt.findPackageFragmentForFile(state.getModule(), file);
            if (fragment == null) {
                LOG.error(new KotlinExceptionWithAttachments(
                        "package fragment is not found for module:" + state.getModule() + " file:" + file)
                        .withPsiAttachment("file.kt", file));
            } else if (!expectedPackageFqName.equals(fragment.getFqName())) {
                LOG.error("expected package fq name: " + expectedPackageFqName + ", actual: " + fragment.getFqName());
            }

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

    @Override
    public PackageFragmentDescriptor getPackageFragment() {
        return packageFragment;
    }
}
