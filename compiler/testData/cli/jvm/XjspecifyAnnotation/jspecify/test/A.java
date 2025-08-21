package test;

public class A {
    public void foo(String x) {}

    @org.jspecify.annotations.Nullable
    public String bar() { return null; }
}
