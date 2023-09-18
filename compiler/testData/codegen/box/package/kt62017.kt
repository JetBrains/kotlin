// TARGET_BACKEND: JVM
// FILE: a/A.java

package a;

abstract class A<T> {
    String o() { return "O"; }
    String k() { return "K"; }
}

// FILE: a/1.kt

package a

private val o = object : A<String>() {}

fun box(): String {
    val k = object : A<String>() {}
    return o.o() + k.k()
}
