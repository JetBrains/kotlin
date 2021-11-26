// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: genericSamProjectedOut.kt
import example.SomeJavaClass

fun test() {
    var a: SomeJavaClass<out String> = SomeJavaClass()
    // a::someFunction parameter has type of Nothing
    // while it's completely safe to pass a lambda for a SAM
    // since Hello is effectively contravariant by its parameter

    a.someFunction {}
    a + {}
    a[{}]
    a += {}
    a[0] = {}
}

// FILE: example/Hello.java
package example;

@FunctionalInterface
public interface Hello<A> {
    void invoke(A a);
}

// FILE: example/SomeJavaClass.java
package example;

public class SomeJavaClass<A> {
    public void someFunction(Hello<A> hello) {
        ((Hello)hello).invoke("OK");
    }

    public SomeJavaClass<A> plus(Hello<A> hello) {
        ((Hello)hello).invoke("OK");
        return this;
    }

    public void get(Hello<A> hello) {
        ((Hello)hello).invoke("OK");
    }

    public void set(int i, Hello<A> hello) {
        ((Hello)hello).invoke("OK");
    }
}
