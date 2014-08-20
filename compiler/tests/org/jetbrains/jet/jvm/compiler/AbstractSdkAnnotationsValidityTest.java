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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.collect.Maps;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JavaBindingContext;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.TypeTransformingVisitor;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AbstractSdkAnnotationsValidityTest extends UsefulTestCase {

    private static final int CLASSES_IN_CHUNK = 100;

    protected abstract JetCoreEnvironment createEnvironment(Disposable parentDisposable);

    protected abstract List<FqName> getClassesToValidate() throws IOException;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TypeTransformingVisitor.setStrictMode(true);
    }

    @Override
    protected void tearDown() throws Exception {
        TypeTransformingVisitor.setStrictMode(false);
        super.tearDown();
    }

    public void testNoErrorsInAlternativeSignatures() throws IOException {
        List<FqName> affectedClasses = getClassesToValidate();

        Map<String, List<String>> errors = Maps.newLinkedHashMap();

        for (int chunkIndex = 0; chunkIndex < affectedClasses.size() / CLASSES_IN_CHUNK + 1; chunkIndex++) {
            Disposable parentDisposable = Disposer.newDisposable();

            try {
                JetCoreEnvironment commonEnvironment = createEnvironment(parentDisposable);

                BindingTrace trace = new BindingTraceContext();
                InjectorForJavaDescriptorResolver injector =
                        InjectorForJavaDescriptorResolverUtil.create(commonEnvironment.getProject(), trace, false);

                BindingContext bindingContext = trace.getBindingContext();

                AlternativeSignatureErrorFindingVisitor visitor = new AlternativeSignatureErrorFindingVisitor(bindingContext, errors);

                int chunkStart = chunkIndex * CLASSES_IN_CHUNK;
                for (FqName javaClass : affectedClasses.subList(chunkStart, Math.min(chunkStart + CLASSES_IN_CHUNK, affectedClasses.size()))) {
                    ClassDescriptor topLevelClass = ResolvePackage.resolveTopLevelClass(injector.getModule(), javaClass);
                    PackageViewDescriptor topLevelPackage = injector.getModule().getPackage(javaClass);
                    if (topLevelClass == null) {
                        continue;
                    }

                    topLevelClass.acceptVoid(visitor);

                    if (topLevelPackage != null) {
                        topLevelPackage.acceptVoid(visitor);
                    }
                }
            }
            finally {
                Disposer.dispose(parentDisposable);
            }
        }


        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Error(s) in SDK alternative signatures: \n");
            for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
                sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
            }
            fail(sb.toString());
        }
    }

    private static class AlternativeSignatureErrorFindingVisitor extends DeclarationDescriptorVisitorEmptyBodies<Void, Void> {
        private final BindingContext bindingContext;
        private final Map<String, List<String>> errors;

        public AlternativeSignatureErrorFindingVisitor(BindingContext bindingContext, Map<String, List<String>> errors) {
            this.bindingContext = bindingContext;
            this.errors = errors;
        }

        @Override
        public Void visitPackageViewDescriptor(PackageViewDescriptor descriptor, Void data) {
            return visitDeclarationRecursively(descriptor, descriptor.getMemberScope());
        }

        @Override
        public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
            // skip java.util.Collection, etc.
            if (!JavaToKotlinClassMap.getInstance().mapPlatformClass(DescriptorUtils.getFqNameSafe(descriptor)).isEmpty()) {
                return null;
            }

            return visitDeclarationRecursively(descriptor, descriptor.getDefaultType().getMemberScope());
        }

        @Override
        public Void visitFunctionDescriptor(FunctionDescriptor descriptor, Void data) {
            return visitDeclaration(descriptor);
        }

        @Override
        public Void visitPropertyDescriptor(PropertyDescriptor descriptor, Void data) {
            return visitDeclaration(descriptor);
        }

        private Void visitDeclaration(@NotNull DeclarationDescriptor descriptor) {
            List<String> errors = bindingContext.get(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor);
            if (errors != null) {
                this.errors.put(DescriptorRenderer.FQ_NAMES_IN_TYPES.render(descriptor), errors);
            }
            return null;
        }

        private Void visitDeclarationRecursively(@NotNull DeclarationDescriptor descriptor, @NotNull JetScope memberScope) {
            for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                member.acceptVoid(this);
            }
            return visitDeclaration(descriptor);
        }
    }
}
