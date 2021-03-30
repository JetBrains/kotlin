// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: intReturnTypeAsNumber.kt

fun box(): String {
    val num = Sam { 42 }
    if (num.get() != 42)
        return "Failed"
    return "OK"
}

// FILE: Sam.java
public interface Sam {
    Number get();
}
