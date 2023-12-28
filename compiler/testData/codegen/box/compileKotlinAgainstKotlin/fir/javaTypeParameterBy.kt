// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib

// FILE: JavaInterface.java
public interface JavaInterface {
    <K extends Comparable<K>> K foo();
}

// FILE: A.kt

class A(j: JavaInterface) : JavaInterface by j


// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val t = object : JavaInterface {
        override fun <K : Comparable<K>> foo(): K = "OK" as K
    }

    return A(t).foo<String>()
}
