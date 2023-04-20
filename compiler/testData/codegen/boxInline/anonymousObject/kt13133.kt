// TARGET_BACKEND: JVM
// WITH_REFLECT
// IGNORE_INLINER_K2: IR
// FILE: 1.kt

package test

inline fun inf(crossinline cif: Any.() -> String): () -> String {
    // Approximate the types manually to avoid running into KT-30696
    val factory: () -> () -> String = {
        object : () -> String {
            override fun invoke() = cif()
        }
    }
    return factory()
}
// FILE: 2.kt

import test.*

fun box(): String {
    val simpleName = inf {
        javaClass.simpleName
    }()

    if (simpleName != "" ) return "fail 1: $simpleName"

    val name = inf {
        javaClass.name
    }()

    if (name != "_2Kt\$box$\$inlined\$inf$2$1" ) return "fail 2: $name"


    return "OK"
}
