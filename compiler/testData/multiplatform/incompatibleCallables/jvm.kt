actual fun f1(): String = ""

actual fun f2(otherName: String) {}

actual fun f3(name: Double) {}
actual fun Double.f3ext() {}

actual fun String.f4() {}

actual fun f5(name: String) {}

actual fun f6(p2: Int) {}

actual fun <K, V> f7() {}

public actual fun f8() {}
internal actual fun f9() {}
private actual fun f10() {}

actual fun <T : Annotation> f11() {}
actual fun <U : MutableList<out String>> f12() {}
actual fun <A, B : Comparable<B>> f13() {}

actual inline fun <reified X> f14() {}
actual inline fun <Y> f15() {}

actual fun f16(s: String = "") {}

actual fun f17(s: Array<out String>) {}
actual fun f18(vararg s: String) {}
actual inline fun f19(crossinline s: () -> Unit) {}
actual inline fun f20(noinline s: () -> Unit) {}
actual fun f21(c: Unit.() -> Unit) {}
actual fun f22(c: suspend Unit.() -> Unit) {}
