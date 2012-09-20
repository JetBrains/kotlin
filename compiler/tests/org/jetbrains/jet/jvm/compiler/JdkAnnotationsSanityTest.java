/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.google.common.collect.Sets;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdkAnnotationsSanityTest extends KotlinTestWithEnvironment {
    private VirtualFile kotlinAnnotationsRoot;

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        kotlinAnnotationsRoot = VirtualFileManager.getInstance().findFileByUrl("file://jdk-annotations");
    }

    public void testNoErrorsInAlternativeSignatures() {
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(BuiltinsScopeExtensionMode.ALL,
                                                                                       getProject());

        final BindingContext bindingContext = injector.getBindingTrace().getBindingContext();
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

        final Map<DeclarationDescriptor, String> errors = Maps.newHashMap();

        Iterable<FqName> affectedClasses = getAffectedClasses(kotlinAnnotationsRoot);
        for (FqName affectedClass : affectedClasses) {
            ClassDescriptor topLevelClass = javaDescriptorResolver.resolveClass(affectedClass);
            assertNotNull("Class has annotation, but it is not found: " + affectedClass, topLevelClass);

            topLevelClass.acceptVoid(new DeclarationDescriptorVisitorEmptyBodies<Void, Void>() {
                @Override
                public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                    for (DeclarationDescriptor member : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
                        member.acceptVoid(this);
                    }

                    return visitDeclaration(descriptor);
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
                    String error = bindingContext.get(BindingContext.ALTERNATIVE_SIGNATURE_DATA_ERROR, descriptor);
                    if (error != null) {
                        errors.put(descriptor, error);
                    }
                    return null;
                }
            });
        }

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Error(s) in JDK alternative signatures: \n");
            for (Map.Entry<DeclarationDescriptor, String> entry : errors.entrySet()) {
                sb.append(DescriptorRenderer.TEXT.render(entry.getKey())).append(" : ").append(entry.getValue()).append("\n");
            }
            fail(sb.toString());
        }
    }

    private static Iterable<FqName> getAffectedClasses(final VirtualFile root) {
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
        return result;
    }
}
