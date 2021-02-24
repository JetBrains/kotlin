// MODULE: lib
// FILE: A.kt

package pkg

interface ClassA {
    companion object {
        val DEFAULT = object : ClassA {
            override fun toString() = "OK"
        }
    }
}

// MODULE: main(lib)
// FILE: B.kt

import pkg.ClassA

fun box(): String {
    val obj = ClassA.DEFAULT
    return obj.toString()
}
