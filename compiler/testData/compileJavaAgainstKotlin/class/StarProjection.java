package test;

public class StarProjection {
    void foo(K<?> k) {
        k.foo(null);
        TestPackage.bar(null);
        new Sub().foo(null);
    }
}