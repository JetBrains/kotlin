// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: A.kt

package a

const val i = 2
const val s = 2.toShort()
const val f = 2.0.toFloat()
const val d = 2.0
const val l = 2L
const val b = 2.toByte()
const val bool = true
const val c = 'c'
const val str = "str"

const val i2 = i
const val s2 = s
const val f2 = f
const val d2 = d
const val l2 = l
const val b2 = b
const val bool2 = bool
const val c2 = c
const val str2 = str

// FILE: B.kt

import a.*

@Ann(i, s, f, d, l, b, bool, c, str)
class MyClass1

@Ann(i2, s2, f2, d2, l2, b2, bool2, c2, str2)
class MyClass2

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(
        val i: Int,
        val s: Short,
        val f: Float,
        val d: Double,
        val l: Long,
        val b: Byte,
        val bool: Boolean,
        val c: Char,
        val str: String
)

fun box(): String {
    // Trigger annotation loading
    (MyClass1() as java.lang.Object).getClass().getAnnotations()
    (MyClass2() as java.lang.Object).getClass().getAnnotations()
    return "OK"
}
