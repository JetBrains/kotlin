// !LANGUAGE: +PartiallySpecifiedTypeArguments
// !DIAGNOSTICS: -UNCHECKED_CAST
// WITH_STDLIB

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

class Foo<K>

class Bar0<K : <!UNRESOLVED_REFERENCE!>_<<!UNRESOLVED_REFERENCE!>_<!>><!>>
class Bar1<K : Foo<<!UNRESOLVED_REFERENCE!>_<!>>>
class Bar2<K : <!UNRESOLVED_REFERENCE!>_<!>>
class Bar3<K> where K : <!UNRESOLVED_REFERENCE!>_<!>
class Bar4<<!UNDERSCORE_IS_RESERVED!>_<!>>

typealias A1<<!UNDERSCORE_IS_RESERVED!>_<!>> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>_<!>
typealias A2<T> = Foo<<!UNRESOLVED_REFERENCE!>_<!>>
typealias A3<T> = (<!UNRESOLVED_REFERENCE!>_<!>) -> T
typealias A4<T> = (T) -> () -> <!UNRESOLVED_REFERENCE!>_<!>
typealias A5<T> = (T) -> (<!UNRESOLVED_REFERENCE!>((_))<!>) -> T

fun foo1(x: <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>) {}
fun foo2(x: Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>>) {}
fun foo3(): <!UNRESOLVED_REFERENCE!>_<!> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun foo5(): Foo<<!UNRESOLVED_REFERENCE!>_<!>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K, <!UNDERSCORE_IS_RESERVED!>_<!>> foo6(): Foo<_> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K : _, <!UNDERSCORE_IS_RESERVED!>_<!>> foo7(): Foo<_> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

class AA1 : <!UNRESOLVED_REFERENCE!>_<!>
class AA2 : <!FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>Foo<<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>_<!>><!>

fun <`_`> bar(): Foo<_> = TODO()

fun test() {
    val x1 = foo<Int, (<!UNRESOLVED_REFERENCE!>_<!>) -> Unit> { { it } }
    val x2 = foo<Int, (Int) -> <!UNRESOLVED_REFERENCE!>_<!>> { { it } }
    val x3 = foo<Int, (<!UNRESOLVED_REFERENCE!>(_)<!>) -> <!UNRESOLVED_REFERENCE!>_<!>> { { it } }
    val x4 = <!FUNCTION_CALL_EXPECTED!>foo<!><!UNRESOLVED_REFERENCE!><<!>Int<!SYNTAX!>, _ -> Float><!> { { <!UNRESOLVED_REFERENCE!>it<!> } }
    val x5 = foo<Int, Foo<(<!UNRESOLVED_REFERENCE!>_<!>) -> Float>> { <!ARGUMENT_TYPE_MISMATCH!>{ it }<!> }
    val x6 = foo<Int, Foo<(<!UNRESOLVED_REFERENCE!>_<!>) -> <!UNRESOLVED_REFERENCE!>_<!>>> { <!ARGUMENT_TYPE_MISMATCH!>{ it }<!> }
    val x7 = foo<Int, Foo<(Int) -> <!UNRESOLVED_REFERENCE!>_<!>>> { <!ARGUMENT_TYPE_MISMATCH!>{ it }<!> }

    val z32: Pair<<!UNRESOLVED_REFERENCE!>_<!>, Float> = 1 to 1f
    val z34: Pair<<!UNRESOLVED_REFERENCE!>((_))<!>, Float> = 1 to 1f

    val x8: (Float) -> Int = { x: <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!> -> 10 }
    val x9: (Foo<Float>) -> Int = { x: Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>> -> 10 }

    val x10 = object : <!UNRESOLVED_REFERENCE!>_<!> {}
    val x11 = object : <!FINAL_SUPERTYPE!>Foo<<!UNRESOLVED_REFERENCE!>_<!>><!>() {}

    if (x11 is <!UNRESOLVED_REFERENCE!>_<!>) { }
    if (<!USELESS_IS_CHECK!>x11 is Foo<<!UNRESOLVED_REFERENCE!>_<!>><!>) { }

    x10 as <!UNRESOLVED_REFERENCE!>_<!>
    x10 <!CAST_NEVER_SUCCEEDS!>as<!> Foo<<!UNRESOLVED_REFERENCE!>_<!>>

    val x12: Foo<@<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!> Int>? = null
    val x13: Foo<@<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>() Int>? = null
    val x14: Foo<@Anno(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>) Int>? = null

    val x15: <!UNRESOLVED_REFERENCE!>_<<!UNRESOLVED_REFERENCE!>_<!>><!>? = null
}

@Target(AnnotationTarget.TYPE)
annotation class Anno(val x: Int)
