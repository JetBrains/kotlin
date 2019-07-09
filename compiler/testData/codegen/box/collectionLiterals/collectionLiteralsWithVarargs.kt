// WITH_REFLECT

// TARGET_BACKEND: JVM

import java.util.Arrays
import kotlin.reflect.KClass
import kotlin.reflect.KFunction0

inline fun <reified T> test(kFunction: KFunction0<Unit>, test: T.() -> Unit) {
    val annotation = kFunction.annotations.single() as T
    annotation.test()
}

fun check(b: Boolean, message: String) {
    if (!b) throw RuntimeException(message)
}

annotation class Foo(vararg val a: String = ["a", "b"])

annotation class Bar(vararg val a: KClass<*> = [Int::class])

@Foo(*["/"])
fun test1() {}

@Bar(*[Long::class, String::class])
fun test2() {}

fun box(): String {
    test<Foo>(::test1) {
        check(a.contentEquals(arrayOf("/")), "Fail 1: ${a.joinToString()}")
    }

    test<Bar>(::test2) {
        check(a.contentEquals(arrayOf(Long::class, String::class)), "Fail 2: ${a.joinToString()}")
    }

    return "OK"
}
