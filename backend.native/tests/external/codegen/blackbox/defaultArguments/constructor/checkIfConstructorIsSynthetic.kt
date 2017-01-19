// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

class A(value: Int = 1)

fun box(): String {
    val constructors = A::class.java.getConstructors().filter { !it.isSynthetic() }
    return if (constructors.size == 2) "OK" else constructors.size.toString()
}
