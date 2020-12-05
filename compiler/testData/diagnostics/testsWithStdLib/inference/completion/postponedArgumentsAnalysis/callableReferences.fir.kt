// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

import kotlin.reflect.*

fun <T>id(x: T) = x

fun <T> select(vararg x: T) = x[0]

fun foo(x: Int) {}
fun foo2(x: Number) {}

fun foo3(x: Int) {}
fun foo3(x: Number) {}

interface A
interface B

fun foo4(x: A) {}
fun foo4(x: B) {}

interface C: A, B

fun foo5(x: A) {}
fun foo5(x: B) {}
fun foo5(x: C) {}

fun <T: (Float) -> Unit> selectNumber(vararg x: T) = x[0]

fun foo6(x: Int) {}
fun foo6(x: Float) {}
fun foo6(x: Number) {}

fun main() {
    select(::foo, { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> })
    select(id(::foo), { x: Number -> }, { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> })

    val x1 = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Int, kotlin.Unit>")!>id(::foo)<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Number, kotlin.Number>")!>id { x: Number -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!> }<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Any>")!>x1<!>

    val x11 = select(id(::foo), id { x: Number -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Any>")!>x11<!>

    select(id(::foo2), id { x: Int -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> })

    select(id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Int, kotlin.Unit>")!>::foo3<!>), id { x: Int -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> })
    select(id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Int, kotlin.Unit>")!>::foo3<!>), id { x: Number -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> })
    select(id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Int, kotlin.Unit>")!>::foo3<!>), id { x: Number -> }, id { x: Int -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> })

    select(id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<A, kotlin.Unit>")!>::foo4<!>), id { x: A -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("A")!>it<!> })
    select(id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<B, kotlin.Unit>")!>::foo4<!>), id { x: B -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("B")!>it<!> })
    // Expected ambiguity
    select(id(<!UNRESOLVED_REFERENCE!>::foo4<!>), id { x: A -> }, id { x: B -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("A & B")!>it<!> })

    select(id(::foo5), id { x: A -> }, id { x: B -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("C")!>it<!> })

    val x2: (Int) -> Unit = selectNumber(id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Number, kotlin.Unit>")!>::foo6<!>), id { x -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*>")!>x<!> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*>")!>it<!> })
}
