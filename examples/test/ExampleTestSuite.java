import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.ExampleTest;

public final class ExampleTestSuite extends UsefulTestCase {

    private String name;
    private ExampleTest tester = new ExampleTest();

    public ExampleTestSuite(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void runTest() throws Exception {
        tester.runBoxTest(getName());
    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory("translator",
                "/testFiles/examples/cases/", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return (new ExampleTestSuite(name));
            }
        });
    }
}
