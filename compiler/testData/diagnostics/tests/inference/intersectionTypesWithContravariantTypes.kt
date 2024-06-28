// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

abstract class Foo<T>

abstract class Bar<T> : Foo<T>(), Comparable<Bar<T>>

fun <T : Comparable<T>, S : T> greater(x: Bar<in S>, t: T): Int = 0
fun <T : Comparable<T>, S : T> greater(x: Bar<in S>, other: Foo<T>): String = ""

fun test(b: Bar<Long>) {
    val result = greater(b, b)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
}
