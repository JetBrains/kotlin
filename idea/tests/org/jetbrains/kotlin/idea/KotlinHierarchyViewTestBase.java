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

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.codeInsight.hierarchy.HierarchyViewTestBase;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.test.KotlinTestUtils;

public abstract class KotlinHierarchyViewTestBase extends HierarchyViewTestBase {
    @Override
    protected void setUp() throws Exception {
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());
        super.setUp();
        ConfigLibraryUtil.configureKotlinRuntime(myModule);
    }

    @Override
    protected void tearDown() throws Exception {
        ConfigLibraryUtil.unConfigureKotlinRuntime(myModule);
        super.tearDown();
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory());
    }
}
