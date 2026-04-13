// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// TARGET_BACKEND: JVM
// ISSUE: KT-84393

// FILE: J.java
public interface J {
    void run();
}

// FILE: test.kt
fun execute(r: J) {
    r.run()
}

var sideEffect = ""

fun test(g: () -> String) {
    execute(g)
}

fun box(): String {
    test { sideEffect += "OK"; "ignored" }
    return sideEffect
}
