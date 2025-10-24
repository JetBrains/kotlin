// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-39697

// FILE: Arguments.java
public class Arguments {
    public static MyData of(Object... args) { return new MyData((String)args[0]); }
}

// FILE: main.kt
class MyData(val s: String)

fun box(): String {
    val a = listOf("O", "K").map(Arguments::of)

    return a[0].s + a[1].s
}
