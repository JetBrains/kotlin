// TARGET_BACKEND: JVM
// v-- fir2ir produces an IrFunctionReference of type KProperty0 instead of an IrPropertyReference
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_REFLECT
// WITH_RUNTIME
// FILE: J.java
public interface J<T> {
    public T getValue();
}

// FILE: box.kt
class Impl(val x: String) : J<String> {
    override fun getValue() = x
}

val j1: J<String> = Impl("O")
// Note that taking a reference to `J<T>::value` is not permitted by the frontend
// in any context except as a direct argument to `by`; e.g. `val x by run { j1::value }`
// would produce an error.
val x by j1::value

fun box(): String {
    val j2: J<String> = Impl("K")
    val y by j2::value
    return x + y
}
