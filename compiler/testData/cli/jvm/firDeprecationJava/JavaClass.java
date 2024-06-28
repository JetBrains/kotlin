import java.util.List;

public class JavaClass {
    @Deprecated
    public String bar(String a, int b, double c) {
        return a + b + c;
    }

    @Deprecated
    public List<String> baz = null;

    public JavaClass() {}

    public JavaClass(String x) {}
}
