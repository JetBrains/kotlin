package org.jetbrains.jet;

import com.intellij.testFramework.UsefulTestCase;

import java.io.File;

/**
 * @author Stepan Koltsov
 */
public abstract class TestCaseWithTmpdir extends UsefulTestCase {

    protected File tmpdir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tmpdir = JetTestUtils.tmpDirForTest(this);
        JetTestUtils.recreateDirectory(tmpdir);
    }

}
