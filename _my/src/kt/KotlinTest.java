package kt;

/**
 * Created by user on 8/8/14.
 */
public class KotlinTest extends AbstractParsingTestCase {
    public KotlinTest() {
        super("", "kt", new KotlinParserDefinition());
    }

    public void testtest() { doTest(true); }

}
