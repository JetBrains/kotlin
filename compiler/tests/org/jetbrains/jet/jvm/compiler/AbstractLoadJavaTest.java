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

import com.intellij.openapi.util.Pair;
import junit.framework.ComparisonFailure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.junit.Assert;

import java.io.File;
import java.util.Arrays;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.analyzeKotlinAndLoadTestNamespace;
import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.compileJavaAndLoadTestNamespaceAndBindingContextFromBinary;
import static org.jetbrains.jet.test.util.NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaceWithFile;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaces;

/*
    The generated test compares namespace descriptors loaded from kotlin sources and read from compiled java.
*/
public abstract class AbstractLoadJavaTest extends TestCaseWithTmpdir {

    public void doTest(@NotNull String javaFileName) throws Exception {
        Assert.assertTrue("A java file expected: " + javaFileName, javaFileName.endsWith(".java"));
        File javaFile = new File(javaFileName);
        File ktFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".kt"));
        File txtFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".txt"));
        NamespaceDescriptor kotlinNamespace = analyzeKotlinAndLoadTestNamespace(ktFile, myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS);
        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext = compileJavaAndLoadTestNamespaceAndBindingContextFromBinary(
                Arrays.asList(javaFile),
                tmpdir, myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS);
        checkLoadedNamespaces(txtFile, kotlinNamespace, javaNamespaceAndContext);
    }

    private static void checkForLoadErrorsAndCompare(
            @NotNull Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext,
            @NotNull Runnable compareNamespacesRunnable
    ) {
        NamespaceDescriptor javaNamespace = javaNamespaceAndContext.first;

        boolean fail = false;
        try {
            ExpectedLoadErrorsUtil.checkForLoadErrors(javaNamespace, javaNamespaceAndContext.second);
        }
        catch (ComparisonFailure e) {
            // to let the next check run even if this one failed
            System.err.println("Expected: " + e.getExpected());
            System.err.println("Actual  : " + e.getActual());
            e.printStackTrace();
            fail = true;
        }
        catch (AssertionError e) {
            e.printStackTrace();
            fail = true;
        }

        compareNamespacesRunnable.run();
        if (fail) {
            fail("See error above");
        }
    }

    public static void checkLoadedNamespaces(
            final File txtFile,
            final NamespaceDescriptor kotlinNamespace,
            final Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext
    ) {
        checkForLoadErrorsAndCompare(javaNamespaceAndContext, new Runnable() {
            @Override
            public void run() {
                compareNamespaces(kotlinNamespace, javaNamespaceAndContext.first, DONT_INCLUDE_METHODS_OF_OBJECT, txtFile);
            }
        });
    }

    public static void checkJavaNamespace(
            final File txtFile,
            final Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext
    ) {
        checkForLoadErrorsAndCompare(javaNamespaceAndContext, new Runnable() {
            @Override
            public void run() {
                compareNamespaceWithFile(javaNamespaceAndContext.first, DONT_INCLUDE_METHODS_OF_OBJECT, txtFile);
            }
        });
    }
}
