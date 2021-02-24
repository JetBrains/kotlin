// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: adaptedFunRefWithDefaultParameters.kt
var ok = "Failed"

fun test(s1: String, s2: String = "K") {
    ok = s1 + s2
}

fun box(): String {
    Sam(::test).foo("O")
    return ok
}

// FILE: Sam.java
public interface Sam {
    void foo(String s);
}
