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

package org.jetbrains.jet.test;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.JetTestUtils;
import org.junit.After;
import org.junit.Before;

import java.io.File;

/**
 * @author Stepan Koltsov
 */
public abstract class TestCaseWithTmpdirIndependentFromIdea {
    protected File tmpdir;

    @Before
    public void before() throws Exception {
        tmpdir = JetTestUtils.tmpDir(this.getClass().getName());
    }

    @After
    public void after() {
        if (tmpdir != null) {
            FileUtil.delete(tmpdir);
        }
    }


}
