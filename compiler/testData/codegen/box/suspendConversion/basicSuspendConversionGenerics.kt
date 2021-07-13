// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// WITH_RUNTIME

class Inv2<T, K>

inline fun <reified T> materialize(): T = T::class.java.newInstance()

inline fun <reified T> foo1(crossinline f: suspend (T) -> String): T = materialize()
inline fun <reified T> foo2(crossinline f: suspend () -> T): T = materialize()
inline fun <reified T, K> foo3(crossinline f: suspend (T) -> K): Inv2<T, K> = Inv2()

fun <T> foo11(f: suspend (T) -> String): T = 1 as T
fun <T> foo21(f: suspend () -> T): T = "" as T
fun <T, K> foo31(f: suspend (T) -> K): Inv2<T, K> = Inv2()

fun <I> id(e: I): I = e

fun test(f: (Int) -> String, g: () -> String) {
    val a0 = foo1(f)
    val a01 = foo11(f)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a0<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a01<!>

    val a1 = foo2(g)
    val a11 = foo21(g)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a11<!>

    val a2 = foo3(f)
    val a21 = foo31(f)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv2<kotlin.Int, kotlin.String>")!>a2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv2<kotlin.Int, kotlin.String>")!>a21<!>

    val a3 = foo1(id(f))
    val a31 = foo11(id(f))
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a31<!>

    val a4 = foo2(id(g))
    val a41 = foo21(id(g))
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a4<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a41<!>

    val a5 = foo3(id(f))
    val a51 = foo31(id(f))
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv2<kotlin.Int, kotlin.String>")!>a5<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv2<kotlin.Int, kotlin.String>")!>a51<!>
}

fun box(): String {
    test({ it.toString() }, { "" })
    return "OK"
}