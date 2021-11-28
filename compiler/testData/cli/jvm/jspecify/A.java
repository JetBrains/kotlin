@org.jspecify.nullness.NullMarked
public class A {
    public void foo(String x) {}

    @org.jspecify.nullness.Nullable
    public String bar() { return null; }
}
