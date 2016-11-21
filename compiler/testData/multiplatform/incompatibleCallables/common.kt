platform fun f1()

platform fun f2(name: String)

platform fun f3(name: String)
platform fun String.f3ext()

platform fun f4(name: String)

platform fun String.f5()

platform fun f6(p1: String, p2: Int)

platform fun <T> f7()

internal platform fun f8()
private platform fun f9()
public platform fun f10()

platform fun <T : Number> f11()
platform fun <U : MutableList<String>> f12()
platform fun <A, B : Continuation<A>> f13()

platform inline fun <X> f14()
platform inline fun <reified Y> f15()

platform fun f16(s: String)

platform fun f17(vararg s: String)
platform fun f18(s: Array<out String>)
platform inline fun f19(crossinline s: () -> Unit)
platform inline fun f20(s: () -> Unit)
platform inline fun f21(noinline s: () -> Unit)
platform inline fun f22(s: () -> Unit)
platform fun f23(coroutine c: Unit.() -> Continuation<Unit>)
platform fun f24(c: Unit.() -> Continuation<Unit>)
