// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// FILE: F.java
import java.util.function.Function;

public class F {
    public static <T, U> U passNull(Function<? super T, ? extends U> f) {
        return f.apply(null);
    }
}

// FILE: test.kt
inline fun <T, U> expectAssertion(f: () -> (T) -> U): Unit? {
    try {
        F.passNull(f())
    } catch (e: NullPointerException) {
        return Unit
    }
    return null
}

fun box(): String {
    expectAssertion {
        fun f(x: String) {}
        ::f
    } ?: return "FAIL: local function"

    expectAssertion {
        class C(val x: String)
        ::C
    } ?: return "FAIL: local class constructor"

    expectAssertion {
        class C(val x: String) {
            fun foo(y: String) {}
        }
        C("")::foo
    } ?: return "FAIL: local class method"

    expectAssertion {
        val o = object {
            fun foo(y: String) {}
        }
        o::foo
    } ?: return "FAIL: anonymous object method"

    expectAssertion {
        { it: String -> }
    } ?: return "FAIL: lambda"

    return "OK"
}
