// WITH_RUNTIME
// FILE: genericSamProjectedOut.kt
import example.SomeJavaClass

fun test(a: SomeJavaClass<out String>) {
    // a::someFunction parameter has type of Nothing
    // while it's completely safe to pass a lambda for a SAM
    // since Hello is effectively contravariant by its parameter

    a.someFunction {}
    a + {}
    a[{}]
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

    public void plus(Hello<A> hello) {
        ((Hello)hello).invoke("OK");
    }

    public void get(Hello<A> hello) {
        ((Hello)hello).invoke("OK");
    }
}
