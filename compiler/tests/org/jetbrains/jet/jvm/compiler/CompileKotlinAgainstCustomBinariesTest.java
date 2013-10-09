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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptorForObject;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.test.util.DescriptorValidator;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static org.jetbrains.jet.test.util.NamespaceComparator.validateAndCompareNamespaceWithFile;

public class CompileKotlinAgainstCustomBinariesTest extends TestCaseWithTmpdir {
    public static final String TEST_DATA_PATH = "compiler/testData/compileKotlinAgainstCustomBinaries/";

    private void doTestWithTxt() throws Exception {
        File ktFile = new File(TEST_DATA_PATH + getTestName(true), getTestName(false) + ".kt");

        NamespaceDescriptor namespace = analyzeFileToNamespace(ktFile);

        NamespaceComparator.Configuration comparator = NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(
                DescriptorValidator.ValidationVisitor.ALLOW_ERROR_TYPES);
        File txtFile = new File(ktFile.getParentFile(), FileUtil.getNameWithoutExtension(ktFile) + ".txt");
        validateAndCompareNamespaceWithFile(namespace, comparator, txtFile);
    }

    @NotNull
    private NamespaceDescriptor analyzeFileToNamespace(@NotNull File ktFile) throws IOException {
        Project project = createEnvironment(ktFile.getParentFile()).getProject();

        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(
                JetTestUtils.loadJetFile(project, ktFile),
                Collections.<AnalyzerScriptParameter>emptyList()
        ).getBindingContext();

        NamespaceDescriptor namespaceDescriptor = bindingContext.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR,
                                                                     LoadDescriptorUtil.TEST_PACKAGE_FQNAME);
        assertNotNull("Failed to find namespace: " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, namespaceDescriptor);
        return namespaceDescriptor;
    }

    @NotNull
    private JetCoreEnvironment createEnvironment(@NotNull File dir) {
        List<File> jarFiles = FileUtil.findFilesByMask(Pattern.compile("^.*\\.jar$"), dir);

        CopyOnWriteArrayList<File> extras = Lists.newCopyOnWriteArrayList();
        extras.addAll(jarFiles);
        extras.add(JetTestUtils.getAnnotationsJar());
        extras.add(dir);

        CompilerConfiguration configurationWithADirInClasspath = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, ArrayUtil.toObjectArray(extras, File.class));

        return new JetCoreEnvironment(getTestRootDisposable(), configurationWithADirInClasspath);
    }

    @NotNull
    private Collection<DeclarationDescriptor> analyzeAndGetAllDescriptors() throws IOException {
        String testName = getTestName(true);
        File ktFile = new File(TEST_DATA_PATH + testName, testName + ".kt");
        return analyzeFileToNamespace(ktFile).getMemberScope().getAllDescriptors();
    }

    public void testDuplicateObjectInBinaryAndSources() throws Exception {
        Collection<DeclarationDescriptor> allDescriptors = analyzeAndGetAllDescriptors();
        assertEquals(allDescriptors.size(), 2);
        for (DeclarationDescriptor descriptor : allDescriptors) {
            assertTrue(descriptor.getName().asString().equals("Lol"));
            assertTrue(descriptor instanceof VariableDescriptorForObject);
            assertFalse("Object property should have valid class",
                        ErrorUtils.isError(((VariableDescriptorForObject) descriptor).getObjectClass()));
        }
    }

    public void testBrokenJarWithNoClassForObjectProperty() throws Exception {
        Collection<DeclarationDescriptor> allDescriptors = analyzeAndGetAllDescriptors();
        assertEquals(allDescriptors.size(), 1);
        DeclarationDescriptor descriptor = allDescriptors.iterator().next();
        assertTrue(descriptor.getName().asString().equals("Lol"));
        assertTrue(descriptor instanceof VariableDescriptorForObject);
        assertTrue("Object property should have an error class",
                   ErrorUtils.isError(((VariableDescriptorForObject) descriptor).getObjectClass()));
    }

    public void testDuplicateLibraries() throws Exception {
        doTestWithTxt();
    }

    public void testMissingEnumReferencedInAnnotation() throws Exception {
        doTestWithTxt();
    }
}
