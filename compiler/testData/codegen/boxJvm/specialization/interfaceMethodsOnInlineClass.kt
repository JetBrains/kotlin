// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.test.assertEquals

interface IBase {
    fun f0()
}

interface I<in T> : IBase {
    fun f1(x: T)
    fun f2(x: T, y: C1)
    fun f3(x: T?, y: C1)
}

interface TakesT<T> {
    fun takeT(x: T): T
    fun takeMaybeT(x: T?): T?
}

@JvmInline
value class C1(val x: String) : I<C1>, TakesT<C1> {
    override fun f0() {}
    override fun f1(x: C1) {}
    override fun f2(x: C1, y: C1) {}
    override fun f3(x: C1?, y: C1) {}
    override fun takeT(x: C1) = x
    override fun takeMaybeT(x: C1?) = x
}

@JvmInline
value class C2(val x: Int) : I<Any>, TakesT<Int> {
    override fun f0() {}
    override fun f1(x: Any) {}
    override fun f2(x: Any, y: C1) {}
    override fun f3(x: Any?, y: C1) {}
    override fun takeT(x: Int) = x
    override fun takeMaybeT(x: Int?) = x
}

fun <@JvmSpecialize T: I<T>> specFun(x: T) {
    x.f0()
    x.f1(x)
    x.f2(x, C1("hi"))
    x.f3(x, C1("hi"))
    x.f3(null, C1("hi"))
}

fun <@JvmSpecialize T: TakesT<Int>> specFun2(x: T) {
    assertEquals(123, x.takeT(123))
    assertEquals(123, x.takeMaybeT(123)!!)
    assertEquals(null, x.takeMaybeT(null))
}

fun <@JvmSpecialize T> specFun3(x: T)
    where T: I<T>,
          T: TakesT<T>
{
    x.f1(x.takeT(x))
    x.f1(x.takeMaybeT(x)!!)
    x.f3(x.takeMaybeT(x), C1("hi"))
    x.f3(x.takeMaybeT(null), C1("hi"))
}

fun box(): String {
    specFun(C1("1"))
    specFun(C2(2))
    specFun2(C2(3))
    specFun3(C1("4"))
    return "OK"
}
