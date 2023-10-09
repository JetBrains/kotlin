// TARGET_BACKEND: JVM
//  ^ TODO: get rid of T::class.java
// IGNORE_BACKEND: JVM

// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: KT-62793

class C

class Inv2<T, K>

inline fun <reified T> materialize(): T = T::class.java.newInstance()

inline fun <reified T> foo1(crossinline f: suspend (T) -> String): T = materialize()
inline fun <reified T> foo2(crossinline f: suspend () -> T): T = materialize()
inline fun <reified T, K> foo3(crossinline f: suspend (T) -> K): Inv2<T, K> = Inv2()

fun <T> foo11(f: suspend (T) -> String): T = C() as T
fun <T> foo21(f: suspend () -> T): T = "" as T
fun <T, K> foo31(f: suspend (T) -> K): Inv2<T, K> = Inv2()

fun <I> id(e: I): I = e

fun test(f: (C) -> String, g: () -> String) {
    val a0 = foo1(f)
    val a01 = foo11(f)

    val a1 = foo2(g)
    val a11 = foo21(g)

    val a2 = foo3(f)
    val a21 = foo31(f)

    val a3 = foo1(id(f))
    val a31 = foo11(id(f))

    val a4 = foo2(id(g))
    val a41 = foo21(id(g))

    val a5 = foo3(id(f))
    val a51 = foo31(id(f))
}

fun box(): String {
    test({ it.toString() }, { "" })
    return "OK"
}