/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.ir;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Processor;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AbstractBlackBoxCodegenTest;
import org.jetbrains.kotlin.codegen.CodegenTestCase;
import org.jetbrains.kotlin.codegen.GeneratedClassLoader;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractIrBlackBoxCodegenTest extends AbstractBlackBoxCodegenTest {
    @Override
    protected void updateConfiguration(CompilerConfiguration configuration) {
        configuration.put(JVMConfigurationKeys.IR, true);
    }
}
