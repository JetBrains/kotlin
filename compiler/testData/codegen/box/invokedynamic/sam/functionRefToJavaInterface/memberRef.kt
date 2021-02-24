// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: memberRef.kt
class C(val t: String) {
    fun test() = t
}

fun box() = Sam(C::test).get(C("OK"))

// FILE: Sam.java
public interface Sam {
    String get(C c);
}
