import kotlin.coroutines.experimental.*

impl fun f1(): String = ""

impl fun f2(otherName: String) {}

impl fun f3(name: Double) {}
impl fun Double.f3ext() {}

impl fun String.f4() {}

impl fun f5(name: String) {}

impl fun f6(p2: Int) {}

impl fun <K, V> f7() {}

public impl fun f8() {}
internal impl fun f9() {}
private impl fun f10() {}

impl fun <T : Annotation> f11() {}
impl fun <U : MutableList<out String>> f12() {}
impl fun <A, B : Continuation<B>> f13() {}

impl inline fun <reified X> f14() {}
impl inline fun <Y> f15() {}

impl fun f16(s: String = "") {}

impl fun f17(s: Array<out String>) {}
impl fun f18(vararg s: String) {}
impl inline fun f19(s: () -> Unit) {}
impl inline fun f20(crossinline s: () -> Unit) {}
impl inline fun f21(s: () -> Unit) {}
impl inline fun f22(noinline s: () -> Unit) {}
impl fun f23(c: Unit.() -> Unit) {}
impl fun f24(c: suspend Unit.() -> Unit) {}
