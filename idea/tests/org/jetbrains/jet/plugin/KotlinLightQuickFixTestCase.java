/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin;

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import org.jetbrains.jet.JetTestCaseBuilder;

abstract public class KotlinLightQuickFixTestCase extends LightQuickFixTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        VfsRootAccess.allowRootAccess(JetTestCaseBuilder.getHomeDirectory());
    }

    @Override
    protected void tearDown() throws Exception {
        VfsRootAccess.disallowRootAccess(JetTestCaseBuilder.getHomeDirectory());
        super.tearDown();
    }
}
