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

package org.jetbrains.jet.builders;

import beans.FunctionDescriptorBean;
import beans.ValueParameterDescriptorBean;
import beans.util.BeanUtil;
import beans.util.CopyProcessor;
import beans.util.DataToBean;
import beans.util.ToString;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BeanCopyProcessorTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    public void testBuiltInFunctions() throws Exception {
        //JetStandardLibrary.initialize();
        printAll(KotlinBuiltIns.getInstance().getBuiltInsScope());
    }

    private void collectAllFunctionDescriptors(JetScope scope, List<FunctionDescriptor> result) {
        for (DeclarationDescriptor descriptor : scope.getAllDescriptors()) {
            if (descriptor instanceof FunctionDescriptor) {
                FunctionDescriptor f = (FunctionDescriptor) descriptor;
                result.add(f);
            }
            else if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                collectAllFunctionDescriptors(classDescriptor.getDefaultType().getMemberScope(), result);
            }
        }
    }

    private void printAll(JetScope scope) {
        StringBuilder builder = new StringBuilder();
        Printer printer = new Printer(builder);
        List<FunctionDescriptor> functions = Lists.newArrayList();
        collectAllFunctionDescriptors(scope, functions);

        Collections.sort(functions, new Comparator<FunctionDescriptor>() {
            @Override
            public int compare(FunctionDescriptor o1, FunctionDescriptor o2) {
                return stripHashCodes(o1.toString()).compareTo(stripHashCodes(o2.toString()));
            }
        });

        for (FunctionDescriptor function : functions) {
            printFunction(function, printer);
        }
        String text = stripHashCodes(builder.toString());
        assertTextEquals(new File("compiler/testData/beanCopy/expected.txt"), text);
    }

    private void printFunction(FunctionDescriptor f, Printer p) {
        p.println("================");
        p.println();
        p.println(f);
        p.println();
        FunctionDescriptorBean functionDescriptorBean = DataToBean.toBean(f);
        FunctionDescriptorBean copy = BeanUtil.deepCopy(functionDescriptorBean);
        FunctionDescriptorBean copy2 = new CopyProcessor().processFunctionDescriptor(functionDescriptorBean);
        String result = functionDescriptorBeanToString(copy);
        String result2 = functionDescriptorBeanToString(copy2);
        assertEquals(stripHashCodes(result), stripHashCodes(result2));
        p.println(result);
    }

    private String functionDescriptorBeanToString(FunctionDescriptorBean copy) {
        ToString toString = new ToString() {
            @Override
            public void processFunctionDescriptor_OverriddenDescriptors(@NotNull FunctionDescriptorBean in, Void out) {
                // Do nothing, they are not ordered, and will disturb the output
            }

            @Override
            public void processValueParameterDescriptor_OverriddenDescriptors(@NotNull ValueParameterDescriptorBean in, Void out) {
                // Do nothing, they are not ordered, and will disturb the output
            }
        };
        toString.processFunctionDescriptor(copy);
        return toString.result();
    }

    private void assertTextEquals(File expectedFile, String actualText) {
        try {
            if (!expectedFile.exists()) {
                expectedFile.getParentFile().mkdirs();
                FileUtil.writeToFile(expectedFile, actualText);
                fail("Expected file does not exist: " + expectedFile + ". Created from actual output");
            }
            else {
                String expectedText = FileUtil.loadFile(expectedFile, true);
                assertEquals(expectedText, actualText);
            }
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private String stripHashCodes(String text) {
        // Removing @12345 from standard toString()'s
        return text.replaceAll("@\\w+", "");
    }
}
