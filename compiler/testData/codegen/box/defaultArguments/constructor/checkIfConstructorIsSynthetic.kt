// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class A(value: Int = 1)

fun box(): String {
    val constructors = A::class.java.getConstructors().filter { !it.isSynthetic() }
    return if (constructors.size == 2) "OK" else constructors.size.toString()
}
