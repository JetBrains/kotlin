package org.jetbrains.jet.resolve;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.lang.psi.JetFile;

import java.io.File;

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
        text = expectedResolveData.extractData(text);
        JetFile jetFile = createPsiFile(new File(filePath).getName(), text);
        expectedResolveData.checkResult(jetFile);
    }
}
