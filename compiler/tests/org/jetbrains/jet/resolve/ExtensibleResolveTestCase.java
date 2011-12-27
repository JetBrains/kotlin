package org.jetbrains.jet.resolve;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class ExtensibleResolveTestCase extends JetLiteFixture {
    private ExpectedResolveData expectedResolveData;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        expectedResolveData = getExpectedResolveData();
    }

    protected abstract ExpectedResolveData getExpectedResolveData();

    protected void doTest(@NonNls String filePath) throws Exception {
        String text = loadFile(filePath);
        List<JetFile> files = JetTestUtils.createTestFiles("file.kt", text, new JetTestUtils.TestFileFactory<JetFile>() {
            @Override
            public JetFile create(String fileName, String text) {
                return expectedResolveData.createFileFromMarkedUpText(fileName, text);
            }
        });
        expectedResolveData.checkResult(files);
    }
}
