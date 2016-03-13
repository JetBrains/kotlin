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

package org.jetbrains.kotlin.test.util;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.lazy.LazyEntity;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.utils.Printer;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class RecursiveDescriptorProcessorTest extends KotlinTestWithEnvironment {
    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable());
    }

    public void testRecursive() throws Exception {
        File ktFile = new File("compiler/testData/recursiveProcessor/declarations.kt");
        File txtFile = new File("compiler/testData/recursiveProcessor/declarations.txt");
        String text = FileUtil.loadFile(ktFile, true);
        KtFile jetFile = KotlinTestUtils.createFile("declarations.kt", text, getEnvironment().getProject());
        AnalysisResult result = KotlinTestUtils.analyzeFile(jetFile, getEnvironment());
        PackageViewDescriptor testPackage = result.getModuleDescriptor().getPackage(FqName.topLevel(Name.identifier("test")));

        List<String> descriptors = recursivelyCollectDescriptors(testPackage);

        StringBuilder builder = new StringBuilder();
        Printer p = new Printer(builder);
        for (String descriptor : descriptors) {
            p.println(descriptor);
        }
        String actualText = builder.toString();

        if (!txtFile.exists()) {
            FileUtil.writeToFile(txtFile, actualText);
            fail("Test data file did not exist and was created from the results of the test: " + txtFile);
        }

        assertSameLinesWithFile(txtFile.getAbsolutePath(), actualText);
    }

    private static Class closestInterface(Class<?> aClass) {
        if (aClass == null) return null;
        if (aClass.isInterface() && aClass != LazyEntity.class) return aClass;

        Class<?>[] interfaces = aClass.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            if (anInterface != LazyEntity.class) return anInterface;
        }

        return closestInterface(aClass.getSuperclass());
    }

    private static List<String> recursivelyCollectDescriptors(PackageViewDescriptor testPackage) {
        final List<String> lines = Lists.newArrayList();
        RecursiveDescriptorProcessor.process(testPackage, null, new DeclarationDescriptorVisitor<Boolean, Void>() {

            private void add(DeclarationDescriptor descriptor) {
                add(descriptor.getName().asString(), descriptor);
            }

            private void add(String name, DeclarationDescriptor descriptor) {
                lines.add(name + ":" + closestInterface(descriptor.getClass()).getSimpleName());
            }

            private void addCallable(CallableMemberDescriptor descriptor) {
                String prefix = descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE ? "fake " : "";
                add(prefix + descriptor.getContainingDeclaration().getName() + "." + descriptor.getName(), descriptor);
            }

            @Override
            public Boolean visitPackageFragmentDescriptor(PackageFragmentDescriptor descriptor, Void data) {
                add(descriptor);
                return true;
            }

            @Override
            public Boolean visitPackageViewDescriptor(PackageViewDescriptor descriptor, Void data) {
                add(descriptor);
                return true;
            }

            @Override
            public Boolean visitVariableDescriptor(VariableDescriptor descriptor, Void data) {
                add(descriptor);
                return true;
            }

            @Override
            public Boolean visitFunctionDescriptor(FunctionDescriptor descriptor, Void data) {
                addCallable(descriptor);
                return true;
            }

            @Override
            public Boolean visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, Void data) {
                add(descriptor);
                return true;
            }

            @Override
            public Boolean visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                add(descriptor);
                return true;
            }

            @Override
            public Boolean visitModuleDeclaration(ModuleDescriptor descriptor, Void data) {
                add(descriptor);
                return true;
            }

            @Override
            public Boolean visitConstructorDescriptor(
                    ConstructorDescriptor constructorDescriptor, Void data
            ) {
                add(constructorDescriptor.getContainingDeclaration().getName() + ".<init>()", constructorDescriptor);
                return true;
            }

            @Override
            public Boolean visitScriptDescriptor(ScriptDescriptor scriptDescriptor, Void data) {
                add(scriptDescriptor);
                return true;
            }

            @Override
            public Boolean visitPropertyDescriptor(PropertyDescriptor descriptor, Void data) {
                addCallable(descriptor);
                return true;
            }

            @Override
            public Boolean visitValueParameterDescriptor(
                    ValueParameterDescriptor descriptor, Void data
            ) {
                add(descriptor);
                return true;
            }

            @Override
            public Boolean visitPropertyGetterDescriptor(
                    PropertyGetterDescriptor descriptor, Void data
            ) {
                addCallable(descriptor);
                return true;
            }

            @Override
            public Boolean visitPropertySetterDescriptor(
                    PropertySetterDescriptor descriptor, Void data
            ) {
                addCallable(descriptor);
                return true;
            }

            @Override
            public Boolean visitReceiverParameterDescriptor(
                    ReceiverParameterDescriptor descriptor, Void data
            ) {
                add(descriptor.getContainingDeclaration().getName() + ".this", descriptor);
                return true;
            }
        });
        Collections.sort(lines);
        return lines;
    }
}
