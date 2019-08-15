@codeanalysis.experimental.annotations.DefaultNotNull
public class A {
    public void foo(String x) {}

    @codeanalysis.experimental.annotations.Nullable
    public String bar() { return null; }
}
