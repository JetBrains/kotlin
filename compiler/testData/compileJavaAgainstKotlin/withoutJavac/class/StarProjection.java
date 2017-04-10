package test;

public class StarProjection {
    void foo(K<?> k) {
        k.foo(null);
        StarProjectionKt.bar(null);
        new Sub().foo(null);
    }
}