// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FILE: Test.java

public class Test {
    public static void checkCallFromJava() {
        try {
            String x = TestKt.foo().iterator().next();
            throw new AssertionError("E should have been thrown");
        } catch (E e) { }
    }
}

// FILE: test.kt

interface MyIterable<T> : Iterable<T>

class E : RuntimeException()
fun foo(): MyIterable<String> = throw E()

fun box(): String {
    try {
        foo().iterator().next()
        return "Fail: E should have been thrown"
    } catch (e: E) {}

    Test.checkCallFromJava()

    return "OK"
}
