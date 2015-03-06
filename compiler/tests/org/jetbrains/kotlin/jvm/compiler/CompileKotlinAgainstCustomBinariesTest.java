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

package org.jetbrains.kotlin.jvm.compiler;

import com.google.common.collect.Iterables;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.CliBaseTest;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorPlainTextToStream;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.test.util.DescriptorValidator;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isNonDefaultObject;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isObject;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile;

public class CompileKotlinAgainstCustomBinariesTest extends TestCaseWithTmpdir {
    public static final String TEST_DATA_PATH = "compiler/testData/compileKotlinAgainstCustomBinaries/";

    @NotNull
    private File getTestDataDirectory() {
        return new File(TEST_DATA_PATH, getTestName(true));
    }

    @NotNull
    private File getTestDataFileWithExtension(@NotNull String extension) {
        return new File(getTestDataDirectory(), getTestName(true) + "." + extension);
    }

    @NotNull
    private File compileLibrary(@NotNull String sourcePath) {
        return MockLibraryUtil.compileLibraryToJar(new File(getTestDataDirectory(), sourcePath).getPath(), "customKotlinLib", false);
    }

    private void doTestWithTxt(@NotNull File... extraClassPath) throws Exception {
        PackageViewDescriptor packageView = analyzeFileToPackageView(extraClassPath);

        RecursiveDescriptorComparator.Configuration comparator =
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(
                        DescriptorValidator.ValidationVisitor.errorTypesAllowed());
        validateAndCompareDescriptorWithFile(packageView, comparator, getTestDataFileWithExtension("txt"));
    }

    @NotNull
    private PackageViewDescriptor analyzeFileToPackageView(@NotNull File... extraClassPath) throws IOException {
        Project project = createEnvironment(Arrays.asList(extraClassPath)).getProject();

        AnalysisResult result = JvmResolveUtil.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
                JetTestUtils.loadJetFile(project, getTestDataFileWithExtension("kt"))
        );

        PackageViewDescriptor packageView = result.getModuleDescriptor().getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME);
        assertNotNull("Failed to find package: " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, packageView);
        return packageView;
    }

    @NotNull
    private JetCoreEnvironment createEnvironment(@NotNull List<File> extraClassPath) {
        List<File> extras = new ArrayList<File>();
        extras.addAll(extraClassPath);
        extras.add(JetTestUtils.getAnnotationsJar());

        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, extras.toArray(new File[extras.size()]));
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    @NotNull
    private Collection<DeclarationDescriptor> analyzeAndGetAllDescriptors(@NotNull File... extraClassPath) throws IOException {
        return analyzeFileToPackageView(extraClassPath).getMemberScope().getAllDescriptors();
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
            throw UtilsPackage.rethrow(e);
        }
    }


    public void testDuplicateObjectInBinaryAndSources() throws Exception {
        Collection<DeclarationDescriptor> allDescriptors = analyzeAndGetAllDescriptors(compileLibrary("library"));
        assertEquals(allDescriptors.toString(), 2, allDescriptors.size());
        for (DeclarationDescriptor descriptor : allDescriptors) {
            assertTrue("Wrong name: " + descriptor, descriptor.getName().asString().equals("Lol"));
            assertTrue("Should be an object: " + descriptor, isObject(descriptor));
        }
    }

    public void testBrokenJarWithNoClassForObject() throws Exception {
        File brokenJar = copyJarFileWithoutEntry(compileLibrary("library"), "test/Lol.class");
        Collection<DeclarationDescriptor> allDescriptors = analyzeAndGetAllDescriptors(brokenJar);
        assertEmpty("No descriptors should be found: " + allDescriptors, allDescriptors);
    }

    public void testSameLibraryTwiceInClasspath() throws Exception {
        doTestWithTxt(compileLibrary("library-1"), compileLibrary("library-2"));
    }

    public void testMissingEnumReferencedInAnnotationArgument() throws Exception {
        doTestWithTxt(copyJarFileWithoutEntry(compileLibrary("library"), "test/E.class"));
    }

    public void testNoWarningsOnJavaKotlinInheritance() throws Exception {
        // This test checks that there are no PARAMETER_NAME_CHANGED_ON_OVERRIDE or DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES
        // warnings when subclassing in Kotlin from Java binaries (in case when no parameter names are available for Java classes)

        JetTestUtils.compileJavaFiles(
                Collections.singletonList(getTestDataFileWithExtension("java")),
                Arrays.asList("-d", tmpdir.getPath())
        );

        Project project = createEnvironment(Collections.singletonList(tmpdir)).getProject();

        AnalysisResult result = JvmResolveUtil.analyzeOneFileWithJavaIntegration(
                JetTestUtils.loadJetFile(project, getTestDataFileWithExtension("kt"))
        );
        result.throwIfError();

        BindingContext bindingContext = result.getBindingContext();
        AnalyzerWithCompilerReport.reportDiagnostics(bindingContext.getDiagnostics(), MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR);

        assertEquals("There should be no diagnostics", 0, Iterables.size(bindingContext.getDiagnostics()));
    }

    public void testIncompleteHierarchyInJava() throws Exception {
        // This test compiles a Java library of two classes (Super and Sub), then deletes Super.class and attempts to compile a Kotlin
        // source against this broken library. The expected result is an "incomplete hierarchy" error message from the compiler

        JetTestUtils.compileJavaFiles(
                Arrays.asList(
                        new File(getTestDataDirectory() + "/library/test/Super.java"),
                        new File(getTestDataDirectory() + "/library/test/Sub.java")
                ),
                Arrays.asList("-d", tmpdir.getPath())
        );

        File superClassFile = new File(tmpdir + "/test/Super.class");
        assert superClassFile.delete() : "Can't delete " + superClassFile;

        File source = new File(getTestDataDirectory(), "source.kt");

        Pair<String, ExitCode> pair = CliBaseTest.executeCompilerGrabOutput(new K2JVMCompiler(), Arrays.asList(
                source.getPath(),
                "-classpath", tmpdir.getPath(),
                "-d", tmpdir.getPath()
        ));
        String output = CliBaseTest.getNormalizedCompilerOutput(pair.first, pair.second, getTestDataDirectory().getPath());

        JetTestUtils.assertEqualsToFile(new File(getTestDataDirectory(), "output.txt"), output);
    }
}
