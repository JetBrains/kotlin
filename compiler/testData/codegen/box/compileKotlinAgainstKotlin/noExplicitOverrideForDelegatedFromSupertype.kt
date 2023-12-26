// TARGET_BACKEND: JVM

// JVM_ABI_K1_K2_DIFF: KT-63828

// MODULE: lib
// FILE: A.kt
package a

interface Named {
    val name: String
}

interface A : Named

// MODULE: main(lib)
// FILE: B.kt
import a.*

open class B(val a: A) : A by a, Named

class C(a: A) : B(a)

fun box(): String {
    return C(object : A {
        override val name: String
            get() = "OK"
    }).name
}
