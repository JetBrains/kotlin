/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CompilerConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class JavaElementFinderTest extends KotlinAsJavaTestBase {

    @Override
    protected List<File> getKotlinSourceRoots() {
        return Collections.singletonList(
                new File("compiler/testData/asJava/findClasses/" + getTestName(false) + ".kt")
        );
    }

    @Override
    protected void tearDown() throws Exception {
        finder = null;
        super.tearDown();
    }

    @Override
    protected void extraConfiguration(@NotNull CompilerConfiguration configuration) {
        JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.runtimeJarForTestsWithJdk8());
    }

    public void testFromEnumEntry() {
        assertClass("Direction");
        assertNoClass("Direction.NORTH");
        assertNoClass("Direction.SOUTH");
        assertNoClass("Direction.WEST");
        // TODO: assertClass("Direction.SOUTH.Hello");
        // TODO: assertClass("Direction.WEST.Some");
    }

    public void testEmptyQualifiedName() {
        assertNoClass("");
        // ROOT package exists
        assertPackage("");
    }

    public void testRepeatableAnnotation() {
        assertClass("RepeatableAnnotation.Container");
        assertNoClass("RepeatableAnnotation2.Container");
        assertNoClass("RepeatableAnnotation2.Container");
    }

    private void assertPackage(String qualifiedName) {
        PsiPackage psiPackage = finder.findPackage(qualifiedName);
        TestCase.assertNotNull(String.format("Package with fqn='%s' wasn't found.", qualifiedName), psiPackage);
        TestCase.assertTrue(String.format("Package with fqn='%s' is not valid.", qualifiedName), psiPackage.isValid());
    }

    private void assertClass(String qualifiedName) {
        PsiClass psiClass = finder.findClass(qualifiedName, GlobalSearchScope.allScope(getProject()));
        TestCase.assertNotNull(String.format("Class with fqn='%s' wasn't found.", qualifiedName), psiClass);
        TestCase.assertTrue(String.format("Class with fqn='%s' is not valid.", qualifiedName), psiClass.isValid());
    }

    private void assertNoClass(String qualifiedName) {
        TestCase.assertNull(String.format("Class with fqn='%s' isn't expected to be found.", qualifiedName),
                            finder.findClass(qualifiedName, GlobalSearchScope.allScope(getProject())));
    }
}
