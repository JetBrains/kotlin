// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: adaptedFunRefWithVararg.kt
var ok = "Failed"

fun test(vararg ss: String) {
    ok = ss[0]
}

fun box(): String {
    Sam(::test).foo("OK")
    return ok
}

// FILE: Sam.java
public interface Sam {
    void foo(String s);
}
