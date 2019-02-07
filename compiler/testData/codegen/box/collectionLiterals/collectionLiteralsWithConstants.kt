// WITH_REFLECT

// TARGET_BACKEND: JVM

import java.util.Arrays
import kotlin.reflect.KFunction0

inline fun <reified T> test(kFunction: KFunction0<Unit>, test: T.() -> Unit) {
    val annotation = kFunction.annotations.single() as T
    annotation.test()
}

fun check(b: Boolean, message: String) {
    if (!b) throw RuntimeException(message)
}

annotation class Foo(val a: IntArray = [], val b: Array<String> = [])

const val ONE_INT = 1
const val ONE_FLOAT = 1f
const val HELLO = "hello"
const val C_CHAR = 'c'

@Foo(
        a = [ONE_INT, ONE_INT + ONE_FLOAT.toInt(), ONE_INT + 10, (ONE_INT % 1.0).toInt()],
        b = [HELLO, HELLO + C_CHAR, HELLO + ", Kotlin", C_CHAR.toString() + C_CHAR])
fun test1() {}

fun box(): String {
    test<Foo>(::test1) {
        check(a.contentEquals(intArrayOf(1, 2, 11, 0)), "Fail 1: ${a.joinToString()}")
        check(b.contentEquals(arrayOf("hello", "helloc", "hello, Kotlin", "cc")), "Fail 2: ${b.joinToString()}")
    }

    return "OK"
}
