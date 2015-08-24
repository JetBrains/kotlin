import kotlin.jvm.jvmName;

public class FakePlatformName {
    @jvmName(name = "fake")
    public String foo() {
        return "foo";
    }

    public String fake() {
        return "fake";
    }
}