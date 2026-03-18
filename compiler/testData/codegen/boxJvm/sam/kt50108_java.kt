// TARGET_BACKEND: JVM
// FILE: test.kt
fun interface IFoo {
    fun foo(s: String)
}

val foo = IFoo {}

fun box(): String {
    try {
        J.callWithNull(foo)
        return "J.callWithNull(foo) should throw NPE"
    } catch (e: NullPointerException) {
        return "OK"
    }
}

// FILE: J.java
public class J {
    public static void callWithNull(IFoo iFoo) {
        iFoo.foo(null);
    }
}
