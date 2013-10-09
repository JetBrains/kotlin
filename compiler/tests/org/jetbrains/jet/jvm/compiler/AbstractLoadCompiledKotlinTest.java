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

import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;

import java.io.File;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME;
import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.compileKotlinToDirAndGetAnalyzeExhaust;
import static org.jetbrains.jet.test.util.RecursiveDescriptorComparator.validateAndCompareDescriptors;

/**
 * Compile Kotlin and then parse model from .class files.
 */
@SuppressWarnings("JUnitTestCaseWithNoTests")
public abstract class AbstractLoadCompiledKotlinTest extends TestCaseWithTmpdir {

    public void doTest(@NotNull String ktFileName) throws Exception {
        doTest(ktFileName, false);
    }

    public void doTestWithAccessors(@NotNull String ktFileName) throws Exception {
        doTest(ktFileName, true);
    }

    private void doTest(@NotNull String ktFileName, boolean includeAccessors) throws Exception {
        File ktFile = new File(ktFileName);
        File txtFile = new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        AnalyzeExhaust exhaust = compileKotlinToDirAndGetAnalyzeExhaust(ktFile, tmpdir, getTestRootDisposable(),
                                                                        ConfigurationKind.JDK_ONLY);

        NamespaceDescriptor namespaceFromSource = exhaust.getBindingContext().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR,
                                                                                  TEST_PACKAGE_FQNAME);
        assert namespaceFromSource != null;
        Assert.assertEquals("test", namespaceFromSource.getName().asString());

        NamespaceDescriptor namespaceFromClass = LoadDescriptorUtil.loadTestNamespaceAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), ConfigurationKind.JDK_ONLY).first;

        checkUsageOfDeserializedScope(namespaceFromClass);

        for (DeclarationDescriptor descriptor : namespaceFromClass.getMemberScope().getAllDescriptors()) {
            if (descriptor instanceof ClassDescriptor) {
                assert descriptor instanceof DeserializedClassDescriptor : DescriptorUtils.getFQName(descriptor) + " is loaded as " + descriptor.getClass();
            }
        }

        validateAndCompareDescriptors(namespaceFromSource, namespaceFromClass,
                                      RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT
                                              .checkPrimaryConstructors(true)
                                              .checkPropertyAccessors(includeAccessors),
                                      txtFile);
    }

    private static void checkUsageOfDeserializedScope(@NotNull NamespaceDescriptor namespaceFromClass) {
        JetScope scope = namespaceFromClass.getMemberScope();
        boolean hasOwnMembers = false;
        for (DeclarationDescriptor declarationDescriptor : scope.getAllDescriptors()) {
            if (declarationDescriptor instanceof CallableMemberDescriptor) {
                hasOwnMembers = true;
            }
        }
        if (hasOwnMembers) {
            assert scope instanceof DeserializedPackageMemberScope : "If namespace has members, members should be inside deserialized scope.";
        }
        else {
            //NOTE: should probably change
            assert !(scope instanceof DeserializedPackageMemberScope) : "We don't use deserialized scopes for namespaces without members.";
        }
    }
}
