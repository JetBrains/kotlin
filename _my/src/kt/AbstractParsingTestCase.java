package kt;

import com.intellij.lang.ParserDefinition;
import com.intellij.testFramework.ParsingTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author gregsh
 */
abstract public class AbstractParsingTestCase extends ParsingTestCase {
    public static final String TEST_DATA_PATH = new File("_my/testData").getAbsolutePath().replace(File.pathSeparatorChar, '/');

    public AbstractParsingTestCase(@NonNls @NotNull String dataPath,
                                   @NotNull String fileExt,
                                   @NotNull ParserDefinition... definitions) {
        super(dataPath, fileExt, definitions);
    }

    @Override
    protected String getTestDataPath() {
        return TEST_DATA_PATH;
    }
}
