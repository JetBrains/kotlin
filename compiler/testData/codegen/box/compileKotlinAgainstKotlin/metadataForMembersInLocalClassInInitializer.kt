// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: KT-69075

// MODULE: lib
// FILE: A.kt

class A {
    val o = object {
        @JvmName("jvmGetO")
        fun getO(): String = "O"
    }

    val k = object {
        @get:JvmName("jvmGetK")
        val k: String = "K"
    }
}

// MODULE: main(lib)
// FILE: B.kt

import kotlin.reflect.full.*

fun box(): String {
    val a = A()
    val obj1 = a.o
    val o = obj1::class.declaredMemberFunctions.single().call(obj1) as String
    val obj2 = a.k
    val k = obj2::class.declaredMemberProperties.single().call(obj2) as String
    return o + k
}
