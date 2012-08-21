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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Pavel Talanov
 */
public final class LoadKotlinDescriptorsTest extends TestCaseWithTmpdir {
    @NotNull
    private final String PATH = "compiler/testData/loadKotlinDescriptors";

    private void doTest(@NotNull String expectedFileName, @NotNull String kotlinFileName) throws Exception {
        NamespaceDescriptor namespaceFromClass = LoadDescriptorUtil.compileKotlin(new File(kotlinFileName), tmpdir, myTestRootDisposable);

        NamespaceComparator.compareNamespaces(namespaceFromClass, namespaceFromClass, NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT, new File(expectedFileName));
    }

    public void testSimpleEnum() throws Exception {
        String dir = PATH + "/enum";
        doTest(dir + "/simpleEnum.txt",
               dir + "/simpleEnum.kt");
    }
}
