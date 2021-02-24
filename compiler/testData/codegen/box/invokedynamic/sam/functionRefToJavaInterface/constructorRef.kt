// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: constructorRef.kt
class C(val t: String)

fun box() = Sam(::C).get("OK").t

// FILE: Sam.java
public interface Sam {
    C get(String s);
}
