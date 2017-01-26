import kotlin.coroutines.experimental.*

header fun f1()

header fun f2(name: String)

header fun f3(name: String)
header fun String.f3ext()

header fun f4(name: String)

header fun String.f5()

header fun f6(p1: String, p2: Int)

header fun <T> f7()

internal header fun f8()
private header fun f9()
public header fun f10()

header fun <T : Number> f11()
header fun <U : MutableList<String>> f12()
header fun <A, B : Continuation<A>> f13()

header inline fun <X> f14()
header inline fun <reified Y> f15()

header fun f16(s: String)

header fun f17(vararg s: String)
header fun f18(s: Array<out String>)
header inline fun f19(crossinline s: () -> Unit)
header inline fun f20(s: () -> Unit)
header inline fun f21(noinline s: () -> Unit)
header inline fun f22(s: () -> Unit)
header fun f23(c: suspend Unit.() -> Unit)
header fun f24(c: Unit.() -> Unit)
