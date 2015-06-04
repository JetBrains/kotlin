import kotlin.platform.platformName;

public class FakePlatformName {
    @platformName(name = "fake")
    public String foo() {
        return "foo";
    }

    public String fake() {
        return "fake";
    }
}