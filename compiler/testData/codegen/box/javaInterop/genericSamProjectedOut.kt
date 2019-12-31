// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
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

// FILE: main.kt
import example.SomeJavaClass

fun box(): String {
    val a: SomeJavaClass<out String> = SomeJavaClass()

    var result = "fail"

    // a::someFunction parameter has type of Nothing
    // while it's completely safe to pass a lambda for a SAM
    // since Hello is effectively contravariant by its parameter
    a.someFunction {
        result = it
    }

    if (result != "OK") return "fail 1: $result"
    result = "fail"

    a + {
        result = it
    }

    if (result != "OK") return "fail 2: $result"
    result = "fail"

    a[{
        result = it
    }]

    if (result != "OK") return "fail 3: $result"

    return "OK"
}
