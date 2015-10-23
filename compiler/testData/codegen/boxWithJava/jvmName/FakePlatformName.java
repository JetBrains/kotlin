import kotlin.jvm.JvmName;

public class FakePlatformName {
    @JvmName(name = "fake")
    public String foo() {
        return "foo";
    }

    public String fake() {
        return "fake";
    }
}