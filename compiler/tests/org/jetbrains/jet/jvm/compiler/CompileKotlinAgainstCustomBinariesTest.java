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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.MockLibraryUtil;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
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
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import static org.jetbrains.jet.test.util.NamespaceComparator.validateAndCompareNamespaceWithFile;

public class CompileKotlinAgainstCustomBinariesTest extends TestCaseWithTmpdir {
    public static final String TEST_DATA_PATH = "compiler/testData/compileKotlinAgainstCustomBinaries/";

    @NotNull
    private File getTestDataDirectory() {
        return new File(TEST_DATA_PATH, getTestName(true));
    }

    @NotNull
    private File compileLibrary(@NotNull String sourcePath) {
        return MockLibraryUtil.compileLibraryToJar(new File(getTestDataDirectory(), sourcePath).getPath());
    }

    private void doTestWithTxt(@NotNull Function0<List<File>> classPathProducer) throws Exception {
        File ktFile = new File(getTestDataDirectory(), getTestName(false) + ".kt");

        NamespaceDescriptor namespace = analyzeFileToNamespace(ktFile, classPathProducer);

        NamespaceComparator.Configuration comparator = NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(
                DescriptorValidator.ValidationVisitor.ALLOW_ERROR_TYPES);
        File txtFile = new File(getTestDataDirectory(), FileUtil.getNameWithoutExtension(ktFile) + ".txt");
        validateAndCompareNamespaceWithFile(namespace, comparator, txtFile);
    }

    @NotNull
    private NamespaceDescriptor analyzeFileToNamespace(@NotNull File ktFile, @NotNull Function0<List<File>> classPathProducer)
            throws IOException {
        Project project = createEnvironment(classPathProducer.invoke()).getProject();

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
    private JetCoreEnvironment createEnvironment(@NotNull List<File> extraClassPath) {
        List<File> extras = new ArrayList<File>();
        extras.addAll(extraClassPath);
        extras.add(JetTestUtils.getAnnotationsJar());

        return new JetCoreEnvironment(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, extras.toArray(new File[extras.size()])));
    }

    @NotNull
    private Collection<DeclarationDescriptor> analyzeAndGetAllDescriptors(@NotNull Function0<List<File>> classPathProducer)
            throws IOException {
        File ktFile = new File(getTestDataDirectory(), getTestName(true) + ".kt");
        return analyzeFileToNamespace(ktFile, classPathProducer).getMemberScope().getAllDescriptors();
    }

    @NotNull
    private static File copyJarFileWithoutEntry(@NotNull File jarPath, @NotNull String entryToDelete) {
        try {
            File outputFile = new File(jarPath.getParentFile(), FileUtil.getNameWithoutExtension(jarPath) + "-after.jar");

            @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
            JarFile jar = new JarFile(jarPath);
            ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            try {
                for (Enumeration<JarEntry> enumeration = jar.entries(); enumeration.hasMoreElements(); ) {
                    JarEntry jarEntry = enumeration.nextElement();
                    if (entryToDelete.equals(jarEntry.getName())) {
                        continue;
                    }
                    output.putNextEntry(jarEntry);
                    output.write(FileUtil.loadBytes(jar.getInputStream(jarEntry)));
                    output.closeEntry();
                }
            }
            finally {
                output.close();
                jar.close();
            }

            return outputFile;
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }


    public void testDuplicateObjectInBinaryAndSources() throws Exception {
        Collection<DeclarationDescriptor> allDescriptors = analyzeAndGetAllDescriptors(new Function0<List<File>>() {
            @Override
            public List<File> invoke() {
                return Collections.singletonList(compileLibrary("library"));
            }
        });
        assertEquals(allDescriptors.size(), 2);
        for (DeclarationDescriptor descriptor : allDescriptors) {
            assertTrue(descriptor.getName().asString().equals("Lol"));
            assertTrue(descriptor instanceof VariableDescriptorForObject);
            assertFalse("Object property should have valid class",
                        ErrorUtils.isError(((VariableDescriptorForObject) descriptor).getObjectClass()));
        }
    }

    public void testBrokenJarWithNoClassForObjectProperty() throws Exception {
        Collection<DeclarationDescriptor> allDescriptors = analyzeAndGetAllDescriptors(new Function0<List<File>>() {
            @Override
            public List<File> invoke() {
                return Collections.singletonList(copyJarFileWithoutEntry(compileLibrary("library"), "test/Lol.class"));
            }
        });
        assertEquals(allDescriptors.size(), 1);
        DeclarationDescriptor descriptor = allDescriptors.iterator().next();
        assertTrue(descriptor.getName().asString().equals("Lol"));
        assertTrue(descriptor instanceof VariableDescriptorForObject);
        assertTrue("Object property should have an error class",
                   ErrorUtils.isError(((VariableDescriptorForObject) descriptor).getObjectClass()));
    }

    public void testDuplicateLibraries() throws Exception {
        doTestWithTxt(new Function0<List<File>>() {
            @Override
            public List<File> invoke() {
                return Arrays.asList(compileLibrary("library-1"), compileLibrary("library-2"));
            }
        });
    }

    public void testMissingEnumReferencedInAnnotation() throws Exception {
        doTestWithTxt(new Function0<List<File>>() {
            @Override
            public List<File> invoke() {
                return Collections.singletonList(copyJarFileWithoutEntry(compileLibrary("library"), "test/E.class"));
            }
        });
    }
}
