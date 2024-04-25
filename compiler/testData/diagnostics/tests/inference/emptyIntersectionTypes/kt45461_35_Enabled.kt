// FIR_IDENTICAL
// LANGUAGE: +ForbidInferringTypeVariablesIntoEmptyIntersection
// WITH_STDLIB

class Foo<T>

class Bar<T>

fun <T> Bar<T>.takeFoo(foo: Foo<out Any?>): Int  = 1

class Inv<O>

fun <K : <!FINAL_UPPER_BOUND!>Inv<out Inv<out Int>><!>> main() {
    fun <T, S : T> Bar<T>.takeFoo(foo: Foo<in S>): String = ""

    val foo = Foo<K>()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>Bar<Inv<in Inv<in Number>>>().takeFoo(foo)<!>
}
