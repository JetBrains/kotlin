/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test;

import com.intellij.codeInsight.completion.CompletionTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("MethodMayBeStatic")
abstract public class CompletionTestCaseWrapper extends CompletionTestCase {
    protected Module createModuleAtWrapper(String moduleName, Project project, ModuleType moduleType, String path) {
        return createModuleAt(moduleName, project, moduleType, path);
    }

    protected Module doCreateRealModuleInWrapper(@NotNull String moduleName, @NotNull Project project, ModuleType moduleType) {
        return doCreateRealModuleIn(moduleName, project, moduleType);
    }
}
