/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import org.jetbrains.kotlin.test.KotlinTestUtils;

abstract public class KotlinDaemonAnalyzerTestCase extends DaemonAnalyzerTestCase {
    @Override
    protected void setUp() throws Exception {
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory());
    }
}
