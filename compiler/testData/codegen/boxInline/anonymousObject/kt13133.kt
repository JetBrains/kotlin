// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: 1.kt

package test

inline fun inf(crossinline cif: Any.() -> String): () -> String {
    return {
        object : () -> String {
            override fun invoke() = cif()
        }
    }()
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

