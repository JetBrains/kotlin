// FILE: nullCheckOnLambdaReturn.kt
fun check(fn: () -> Any) = fn()

fun <T> id(x: T) = x

fun test1() = check { J.foo() }

val test2: () -> Any = { J.foo() }

val test3: () -> Any = { J.foo() } as () -> Any

val test4: () -> Any = id { J.foo() }

// FILE: J.java
public class J {
    public static String foo() { return null; }
}