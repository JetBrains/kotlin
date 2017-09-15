expect fun f1()

expect fun f2(name: String)

expect fun f3(name: String)
expect fun String.f3ext()

expect fun f4(name: String)

expect fun String.f5()

expect fun f6(p1: String, p2: Int)

expect fun <T> f7()

internal expect fun f8()
private expect fun f9()
public expect fun f10()

expect fun <T : Number> f11()
expect fun <U : MutableList<String>> f12()
expect fun <A, B : Comparable<A>> f13()

expect inline fun <X> f14()
expect inline fun <reified Y> f15()

expect fun f16(s: String)

expect fun f17(vararg s: String)
expect fun f18(s: Array<out String>)
expect inline fun f19(s: () -> Unit)
expect inline fun f20(s: () -> Unit)
expect fun f21(c: suspend Unit.() -> Unit)
expect fun f22(c: Unit.() -> Unit)
