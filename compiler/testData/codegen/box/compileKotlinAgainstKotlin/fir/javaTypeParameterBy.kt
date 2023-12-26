// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib

// JVM_ABI_K1_K2_DIFF: KT-63870

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
