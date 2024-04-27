// TARGET_BACKEND: JVM
// WITH_STDLIB

class A(value: Int = 1)

fun box(): String {

    return if (val constructors = A::class.java.getConstructors().filter { !it.isSynthetic() }; constructors.size == 2) "OK" else constructors.size.toString()
}
