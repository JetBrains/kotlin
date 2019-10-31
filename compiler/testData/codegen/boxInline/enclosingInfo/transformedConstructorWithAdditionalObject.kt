// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test

interface Z<T> {
    fun a() : T
}

inline fun test(crossinline z: () -> String) =
        object : Z<Z<String>> {

            val p: Z<String> = object : Z<String> {

                val p2 = z()

                override fun a() = p2
            }

            override fun a() = p
        }

// FILE: 2.kt

import test.*

fun box(): String {
    var z = "OK"
    val res = test {
        z
    }


    val javaClass1 = res.javaClass
    val enclosingMethod = javaClass1.enclosingMethod
    if (enclosingMethod?.name != "box") return "fail 1: ${enclosingMethod?.name}"

    val enclosingClass = javaClass1.enclosingClass
    if (enclosingClass?.name != "_2Kt") return "fail 2: ${enclosingClass?.name}"


    val res2 = res.a()
    val enclosingConstructor = res2.javaClass.enclosingConstructor
    if (enclosingConstructor?.name != javaClass1.name) return "fail 3: ${enclosingConstructor?.name} != ${javaClass1.name}"

    val enclosingClass2 = res2.javaClass.enclosingClass
    if (enclosingClass2?.name != javaClass1.name) return "fail 4: ${enclosingClass2?.name} != ${javaClass1.name}"



    return res2.a()
}
