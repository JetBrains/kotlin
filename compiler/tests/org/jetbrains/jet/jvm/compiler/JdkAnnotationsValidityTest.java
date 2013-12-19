/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaBindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.TypeTransformingVisitor;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public class JdkAnnotationsValidityTest extends UsefulTestCase {

    private static final int CLASSES_IN_CHUNK = 500;

    private static JetCoreEnvironment createEnvironment(Disposable parentDisposable) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK, JetTestUtils.getAnnotationsJar());
        configuration.add(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, new File("ideaSDK/lib/jdkAnnotations.jar"));
        return JetCoreEnvironment.createForTests(parentDisposable, configuration);
    }

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

    public void testNoErrorsInAlternativeSignatures() {
        List<FqName> affectedClasses = getAffectedClasses("file://jdk-annotations");

        Map<String, List<String>> errors = Maps.newLinkedHashMap();

        for (int chunkIndex = 0; chunkIndex < affectedClasses.size() / CLASSES_IN_CHUNK + 1; chunkIndex++) {
            Disposable parentDisposable = Disposer.newDisposable();

            try {
                JetCoreEnvironment commonEnvironment = createEnvironment(parentDisposable);

                BindingTrace trace = new BindingTraceContext();
                InjectorForJavaDescriptorResolver injector =
                        InjectorForJavaDescriptorResolverUtil.create(commonEnvironment.getProject(), trace);

                BindingContext bindingContext = trace.getBindingContext();
                JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

                AlternativeSignatureErrorFindingVisitor visitor = new AlternativeSignatureErrorFindingVisitor(bindingContext, errors);

                int chunkStart = chunkIndex * CLASSES_IN_CHUNK;
                for (FqName javaClass : affectedClasses.subList(chunkStart, Math.min(chunkStart + CLASSES_IN_CHUNK, affectedClasses.size()))) {
                    ClassDescriptor topLevelClass = javaDescriptorResolver.resolveClass(javaClass, IGNORE_KOTLIN_SOURCES);
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
            StringBuilder sb = new StringBuilder("Error(s) in JDK alternative signatures: \n");
            for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
                sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
            }
            fail(sb.toString());
        }
    }

    static List<FqName> getAffectedClasses(String rootUrl) {
        Disposable myDisposable = Disposer.newDisposable();

        try {
            createEnvironment(myDisposable);

            VirtualFile root = VirtualFileManager.getInstance().findFileByUrl(rootUrl);
            assert root != null;

            final Set<FqName> result = Sets.newLinkedHashSet();
            VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    if (ExternalAnnotationsManager.ANNOTATIONS_XML.equals(file.getName())) {
                        try {
                            String text = StreamUtil.readText(file.getInputStream());
                            Matcher matcher = Pattern.compile("<item name=['\"]([\\w\\d\\.]+)[\\s'\"]").matcher(text);
                            while (matcher.find()) {
                                result.add(new FqName(matcher.group(1)));
                            }
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return true;
                }
            });

            return Lists.newArrayList(result);
        }
        finally {
            Disposer.dispose(myDisposable);
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
                this.errors.put(DescriptorRenderer.TEXT.render(descriptor), errors);
            }
            return null;
        }

        private Void visitDeclarationRecursively(@NotNull DeclarationDescriptor descriptor, @NotNull JetScope memberScope) {
            for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                if (member instanceof DeclarationDescriptorWithVisibility
                    && ((DeclarationDescriptorWithVisibility) member).getVisibility().isPublicAPI()) {
                    member.acceptVoid(this);
                }
            }

            return visitDeclaration(descriptor);
        }
    }
}
