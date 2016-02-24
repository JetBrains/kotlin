// FILE: A.kt

package pkg

interface ClassA {
    companion object {
        val DEFAULT = object : ClassA {
            override fun toString() = "OK"
        }
    }
}

// FILE: B.kt

import pkg.ClassA

fun main(args: Array<String>) {
    val obj = ClassA.DEFAULT
    obj.toString()
}
