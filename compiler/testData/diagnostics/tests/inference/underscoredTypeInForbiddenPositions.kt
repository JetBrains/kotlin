// !LANGUAGE: +PartiallySpecifiedTypeArguments
// !DIAGNOSTICS: -UNCHECKED_CAST
// WITH_STDLIB

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

class Foo<K>

class Bar0<K : <!UNRESOLVED_REFERENCE!>_<!><<!UNRESOLVED_REFERENCE!>_<!>>>
class Bar1<K : Foo<<!UNRESOLVED_REFERENCE!>_<!>>>
class Bar2<K : <!UNRESOLVED_REFERENCE!>_<!>>
class Bar3<K> where K : <!UNRESOLVED_REFERENCE!>_<!>
class Bar4<<!UNDERSCORE_IS_RESERVED!>_<!>>

typealias A1<<!UNDERSCORE_IS_RESERVED!>_<!>> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS, UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>
typealias A11<<!UNDERSCORE_IS_RESERVED!>_<!>> = Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>
typealias A12<<!UNDERSCORE_IS_RESERVED!>_<!>> = Foo<Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>>
typealias A2<T> = Foo<<!UNRESOLVED_REFERENCE!>_<!>>
typealias A3<T> = (<!UNRESOLVED_REFERENCE!>_<!>) -> T
typealias A4<T> = (T) -> () -> <!UNRESOLVED_REFERENCE!>_<!>
typealias A5<T> = (T) -> (((<!UNRESOLVED_REFERENCE!>_<!>))) -> T

fun foo1(x: <!UNRESOLVED_REFERENCE!>_<!>) {}
fun foo2(x: Foo<<!UNRESOLVED_REFERENCE!>_<!>>) {}
fun foo21(x: Foo<Foo<<!UNRESOLVED_REFERENCE!>_<!>>>) {}
fun foo3(): <!UNRESOLVED_REFERENCE!>_<!> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun foo4(): Foo<<!UNRESOLVED_REFERENCE!>_<!>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun foo5(): Foo<Foo<<!UNRESOLVED_REFERENCE!>_<!>>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K, <!UNDERSCORE_IS_RESERVED!>_<!>> foo6(): Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K : <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>, <!UNDERSCORE_IS_RESERVED!>_<!>> foo7(): <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K : Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>, <!UNDERSCORE_IS_RESERVED!>_<!>> foo8(): Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K : Foo<Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>>, <!UNDERSCORE_IS_RESERVED!>_<!>> foo9(): Foo<Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <!UNRESOLVED_REFERENCE!>_<!>.foo10() {}
fun Foo<<!UNRESOLVED_REFERENCE!>_<!>>.foo11() {}
fun Foo<Foo<<!UNRESOLVED_REFERENCE!>_<!>>>.foo12() {}

class AA1 : <!UNRESOLVED_REFERENCE!>_<!>
class AA2 : <!FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>Foo<<!UNRESOLVED_REFERENCE!>_<!>><!>

fun <`_`> bar(): Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>> = TODO()
fun <`_`> bar1(): Foo<Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>> = TODO()

fun test() {
    val x1 = foo<Int, (<!UNRESOLVED_REFERENCE!>_<!>) -> Unit> { { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!> } }
    val x2 = foo<Int, (Int) -> <!UNRESOLVED_REFERENCE!>_<!>> { { it } }
    val x3 = foo<Int, ((<!UNRESOLVED_REFERENCE!>_<!>)) -> <!UNRESOLVED_REFERENCE!>_<!>> { { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!> } }
    val x4 = <!FUNCTION_CALL_EXPECTED, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>foo<!><!DEBUG_INFO_MISSING_UNRESOLVED!><<!>Int<!SYNTAX!>, _ -> Float><!> { { <!UNRESOLVED_REFERENCE!>it<!> } }
    val x5 = foo<Int, Foo<(<!UNRESOLVED_REFERENCE!>_<!>) -> Float>> { <!TYPE_MISMATCH!>{ <!TYPE_MISMATCH!>it<!> }<!> }
    val x6 = foo<Int, Foo<(<!UNRESOLVED_REFERENCE!>_<!>) -> <!UNRESOLVED_REFERENCE!>_<!>>> { <!TYPE_MISMATCH!>{ <!TYPE_MISMATCH!>it<!> }<!> }
    val x7 = foo<Int, Foo<(Int) -> <!UNRESOLVED_REFERENCE!>_<!>>> { <!TYPE_MISMATCH!>{ <!TYPE_MISMATCH!>it<!> }<!> }

    val z32: Pair<<!UNRESOLVED_REFERENCE!>_<!>, Float> = 1 to 1f
    val z34: Pair<((<!UNRESOLVED_REFERENCE!>_<!>)), Float> = 1 to 1f

    val x8: (Float) -> Int = { x: <!UNRESOLVED_REFERENCE!>_<!> -> 10 }
    val x9: (Foo<Float>) -> Int = { x: Foo<<!UNRESOLVED_REFERENCE!>_<!>> -> 10 }

    val x10 = object : <!UNRESOLVED_REFERENCE!>_<!> {}
    val x11 = object : <!FINAL_SUPERTYPE!>Foo<<!UNRESOLVED_REFERENCE!>_<!>><!>() {}

    if (x11 is <!UNRESOLVED_REFERENCE!>_<!>) { }
    if (x11 is <!INCOMPATIBLE_TYPES!>Foo<<!UNRESOLVED_REFERENCE!>_<!>><!>) { }

    x10 as <!UNRESOLVED_REFERENCE!>_<!>
    x10 <!CAST_NEVER_SUCCEEDS!>as<!> Foo<<!UNRESOLVED_REFERENCE!>_<!>>

    val x12: Foo<@<!UNRESOLVED_REFERENCE!>_<!> Int>? = null
    val x13: Foo<@<!UNRESOLVED_REFERENCE!>_<!>() Int>? = null
    val x14: Foo<@Anno(<!UNRESOLVED_REFERENCE!>_<!>) Int>? = null

    val x15: <!UNRESOLVED_REFERENCE!>_<!><<!UNRESOLVED_REFERENCE!>_<!>>? = null
}

@Target(AnnotationTarget.TYPE)
annotation class Anno(val x: Int)
