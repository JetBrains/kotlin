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
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import kotlin.Pair;
import kotlin.collections.SetsKt;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.AbstractCliTest;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.test.util.DescriptorValidator;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.StringsKt;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.junit.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isObject;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile;

public class CompileKotlinAgainstCustomBinariesTest extends TestCaseWithTmpdir {
    private static final String TEST_DATA_PATH = "compiler/testData/compileKotlinAgainstCustomBinaries/";
    private static final Pattern JAVA_FILES = Pattern.compile(".*\\.java$");

    @NotNull
    private File getTestDataDirectory() {
        return new File(TEST_DATA_PATH, getTestName(true));
    }

    @NotNull
    private File getTestDataFileWithExtension(@NotNull String extension) {
        return new File(getTestDataDirectory(), getTestName(true) + "." + extension);
    }

    @NotNull
    private File compileLibrary(@NotNull String sourcePath, @NotNull File... extraClassPath) {
        File result = new File(tmpdir, sourcePath + ".jar");
        Pair<String, ExitCode> output = compileKotlin(sourcePath, result, extraClassPath);
        Assert.assertEquals(normalizeOutput(new Pair<String, ExitCode>("", ExitCode.OK)), normalizeOutput(output));
        return result;
    }

    @NotNull
    private String normalizeOutput(@NotNull Pair<String, ExitCode> output) {
        return AbstractCliTest.getNormalizedCompilerOutput(
                output.getFirst(), output.getSecond(), getTestDataDirectory().getPath(), JvmMetadataVersion.INSTANCE
        );
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
        KotlinCoreEnvironment environment = createEnvironment(Arrays.asList(extraClassPath));

        AnalysisResult result = JvmResolveUtil.analyzeAndCheckForErrors(
                KotlinTestUtils.loadJetFile(environment.getProject(), getTestDataFileWithExtension("kt")), environment
        );

        PackageViewDescriptor packageView = result.getModuleDescriptor().getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME);
        assertFalse("Failed to find package: " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, packageView.isEmpty());
        return packageView;
    }

    @NotNull
    private KotlinCoreEnvironment createEnvironment(@NotNull List<File> extraClassPath) {
        List<File> extras = new ArrayList<File>();
        extras.addAll(extraClassPath);
        extras.add(KotlinTestUtils.getAnnotationsJar());

        CompilerConfiguration configuration =
                KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, extras.toArray(new File[extras.size()]));
        return KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    @NotNull
    private Collection<DeclarationDescriptor> analyzeAndGetAllDescriptors(@NotNull File... extraClassPath) throws IOException {
        return DescriptorUtils.getAllDescriptors(analyzeFileToPackageView(extraClassPath).getMemberScope());
    }

    @NotNull
    private static File copyJarFileWithoutEntry(@NotNull File jarPath, @NotNull String... entriesToDelete) {
        try {
            File outputFile = new File(jarPath.getParentFile(), FileUtil.getNameWithoutExtension(jarPath) + "-after.jar");
            Set<String> toDelete = SetsKt.setOf(entriesToDelete);

            @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
            JarFile jar = new JarFile(jarPath);
            ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            try {
                for (Enumeration<JarEntry> enumeration = jar.entries(); enumeration.hasMoreElements(); ) {
                    JarEntry jarEntry = enumeration.nextElement();
                    if (toDelete.contains(jarEntry.getName())) {
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
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    private File compileJava(@NotNull String libraryDir) throws Exception {
        List<File> allJavaFiles = FileUtil.findFilesByMask(JAVA_FILES, new File(getTestDataDirectory(), libraryDir));
        File result = new File(tmpdir, libraryDir);
        assert result.mkdirs() : "Could not create directory: " + result;
        KotlinTestUtils.compileJavaFiles(allJavaFiles, Arrays.asList("-d", result.getPath()));
        return result;
    }

    @NotNull
    private static File deletePaths(@NotNull File library, @NotNull String... pathsToDelete) {
        for (String pathToDelete : pathsToDelete) {
            File fileToDelete = new File(library, pathToDelete);
            assert fileToDelete.delete() : "Can't delete " + fileToDelete;
        }
        return library;
    }

    @NotNull
    private Pair<String, ExitCode> compileKotlin(
            @NotNull String fileName,
            @NotNull File output,
            @NotNull File... classpath
    ) {
        List<String> args = new ArrayList<String>();
        File sourceFile = new File(getTestDataDirectory(), fileName);
        assert sourceFile.exists() : "Source file does not exist: " + sourceFile.getAbsolutePath();
        args.add(sourceFile.getPath());
        if (classpath.length > 0) {
            args.add("-classpath");
            args.add(StringsKt.join(Arrays.asList(classpath), File.pathSeparator));
        }
        args.add("-d");
        args.add(output.getPath());

        return AbstractCliTest.executeCompilerGrabOutput(new K2JVMCompiler(), args);
    }

    private void doTestBrokenJavaLibrary(@NotNull String libraryName, @NotNull String... pathsToDelete) throws Exception {
        // This function compiles a Java library, then deletes one class file and attempts to compile a Kotlin source against
        // this broken library. The expected result is an error message from the compiler
        File library = deletePaths(compileJava(libraryName), pathsToDelete);

        Pair<String, ExitCode> output = compileKotlin("source.kt", tmpdir, library);
        KotlinTestUtils.assertEqualsToFile(new File(getTestDataDirectory(), "output.txt"), normalizeOutput(output));
    }

    private void doTestBrokenKotlinLibrary(@NotNull String libraryName, @NotNull String... pathsToDelete) throws Exception {
        // Analogous to doTestBrokenJavaLibrary, but with a Kotlin library compiled to a JAR file
        File library = copyJarFileWithoutEntry(compileLibrary(libraryName), pathsToDelete);
        Pair<String, ExitCode> output = compileKotlin("source.kt", tmpdir, library);
        KotlinTestUtils.assertEqualsToFile(new File(getTestDataDirectory(), "output.txt"), normalizeOutput(output));
    }

    // ------------------------------------------------------------------------------

    public void testRawTypes() throws Exception {
        KotlinTestUtils.compileJavaFiles(
                Collections.singletonList(
                        new File(getTestDataDirectory() + "/library/test/A.java")
                ),
                Arrays.asList("-d", tmpdir.getPath())
        );

        Pair<String, ExitCode> outputLib = compileKotlin("library/test/lib.kt", tmpdir, tmpdir);

        Pair<String, ExitCode> outputMain = compileKotlin("main.kt", tmpdir, tmpdir);

        KotlinTestUtils.assertEqualsToFile(
                new File(getTestDataDirectory(), "output.txt"), normalizeOutput(outputLib) + "\n" + normalizeOutput(outputMain)
        );
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

        KotlinTestUtils.compileJavaFiles(
                Collections.singletonList(getTestDataFileWithExtension("java")),
                Arrays.asList("-d", tmpdir.getPath())
        );

        KotlinCoreEnvironment environment = createEnvironment(Collections.singletonList(tmpdir));

        AnalysisResult result = JvmResolveUtil.analyze(
                KotlinTestUtils.loadJetFile(environment.getProject(), getTestDataFileWithExtension("kt")), environment
        );
        result.throwIfError();

        BindingContext bindingContext = result.getBindingContext();
        AnalyzerWithCompilerReport.Companion.reportDiagnostics(
                bindingContext.getDiagnostics(),
                new PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
        );

        assertEquals("There should be no diagnostics", 0, Iterables.size(bindingContext.getDiagnostics()));
    }

    public void testIncompleteHierarchyInJava() throws Exception {
        doTestBrokenJavaLibrary("library", "test/Super.class");
    }

    public void testIncompleteHierarchyInKotlin() throws Exception {
        doTestBrokenKotlinLibrary("library", "test/Super.class");
    }

    public void testMissingDependencySimple() throws Exception {
        doTestBrokenKotlinLibrary("library", "a/A.class");
    }

    public void testMissingDependencyDifferentCases() throws Exception {
        doTestBrokenKotlinLibrary("library", "a/A.class");
    }

    public void testMissingDependencyNestedAnnotation() throws Exception {
        doTestBrokenKotlinLibrary("library", "a/A$Anno.class");
    }

    public void testMissingDependencyConflictingLibraries() throws Exception {
        File library1 = copyJarFileWithoutEntry(compileLibrary("library1"),
                                                "a/A.class", "a/A$Inner.class", "a/AA.class", "a/AA$Inner.class");
        File library2 = copyJarFileWithoutEntry(compileLibrary("library2"),
                                                "a/A.class", "a/A$Inner.class", "a/AA.class", "a/AA$Inner.class");
        Pair<String, ExitCode> output = compileKotlin("source.kt", tmpdir, library1, library2);
        KotlinTestUtils.assertEqualsToFile(new File(getTestDataDirectory(), "output.txt"), normalizeOutput(output));
    }

    public void testMissingDependencyJava() throws Exception {
        doTestBrokenJavaLibrary("library", "test/Bar.class");
    }

    public void testMissingDependencyJavaConflictingLibraries() throws Exception {
        File library1 = deletePaths(compileJava("library1"), "test/A.class", "test/A$Inner.class");
        File library2 = deletePaths(compileJava("library2"), "test/A.class", "test/A$Inner.class");
        Pair<String, ExitCode> output = compileKotlin("source.kt", tmpdir, library1, library2);
        KotlinTestUtils.assertEqualsToFile(new File(getTestDataDirectory(), "output.txt"), normalizeOutput(output));
    }

    public void testMissingDependencyJavaNestedAnnotation() throws Exception {
        doTestBrokenJavaLibrary("library", "test/A$Anno.class");
    }

    /*test source mapping generation when source info is absent*/
    public void testInlineFunWithoutDebugInfo() throws Exception {
        compileKotlin("sourceInline.kt", tmpdir);

        File inlineFunClass = new File(tmpdir.getAbsolutePath(), "test/A.class");
        ClassWriter cw = new ClassWriter(Opcodes.ASM5);
        new ClassReader(FilesKt.readBytes(inlineFunClass)).accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public void visitSource(String source, String debug) {
                //skip debug info
            }
        }, 0);

        assert inlineFunClass.delete();
        assert !inlineFunClass.exists();

        FilesKt.writeBytes(inlineFunClass, cw.toByteArray());

        compileKotlin("source.kt", tmpdir, tmpdir);

        final Ref<String> debugInfo = new Ref<String>();
        File resultFile = new File(tmpdir.getAbsolutePath(), "test/B.class");
        new ClassReader(FilesKt.readBytes(resultFile)).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public void visitSource(String source, String debug) {
                //skip debug info
                debugInfo.set(debug);
            }
        }, 0);

        String expected = "SMAP\n" +
                          "source.kt\n" +
                          "Kotlin\n" +
                          "*S Kotlin\n" +
                          "*F\n" +
                          "+ 1 source.kt\n" +
                          "test/B\n" +
                          "*L\n" +
                          "1#1,13:1\n" +
                          "*E\n";

        if (InlineCodegenUtil.GENERATE_SMAP) {
            assertEquals(expected, debugInfo.get());
        }
        else {
            assertEquals(null, debugInfo.get());
        }
    }

    public void testReplaceAnnotationClassWithInterface() throws Exception {
        File library1 = compileLibrary("library-1");
        File usage = compileLibrary("usage", library1);
        File library2 = compileLibrary("library-2");
        doTestWithTxt(usage, library2);
    }
}
