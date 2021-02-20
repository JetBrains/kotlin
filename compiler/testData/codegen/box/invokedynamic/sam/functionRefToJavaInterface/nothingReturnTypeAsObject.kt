// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: nothingReturnTypeAsObject.kt

fun interface IFoo<T> {
    fun foo(): T
}

fun thr(s: String): Nothing = throw RuntimeException(s)

fun box(): String {
    try {
        Sam(::thr).get("OK")
    } catch (e: RuntimeException) {
        return e.message!!
    }
    return "Failed"
}


// FILE: Sam.java
public interface Sam {
    Object get(String s);
}
