// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER
class A

@Target(AnnotationTarget.TYPE)
annotation class x

fun @x A.foo(a: @x Int) {
    val v: @x Int = 1
}

fun <T> @x List<@x T>.foo(l: List<@x T>): @x List<@x T> = throw Exception()

fun <T, U: T> List<@x T>.firstTyped(): U = throw Exception()

val <T> @x List<@x T>.f: Int get() = 42