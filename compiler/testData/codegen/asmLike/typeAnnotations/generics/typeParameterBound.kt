// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// WITH_STDLIB

package foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

interface Inv<T>
interface In<in U>
interface Out<out V>

fun <T01 : Inv<@Ann Number>> f01() {}
fun <T02 : In<@Ann Number>> f02() {}
fun <T03 : Out<@Ann Number>> f03() {}

fun <T04 : Inv<Inv<@Ann Number>>> f04() {}
fun <T05 : Inv<In<@Ann Number>>> f05() {}
fun <T06 : Inv<Out<@Ann Number>>> f06() {}
fun <T07 : In<Inv<@Ann Number>>> f07() {}
fun <T08 : In<In<@Ann Number>>> f08() {}
fun <T09 : In<Out<@Ann Number>>> f09() {}
fun <T10 : Out<Inv<@Ann Number>>> f10() {}
fun <T11 : Out<In<@Ann Number>>> f11() {}
fun <T12 : Out<Out<@Ann Number>>> f12() {}

fun <T13 : Inv<in @Ann Number>> f13() {}
fun <T14 : Inv<out @Ann Number>> f14() {}
fun <T15 : In<in @Ann Number>> f15() {}
fun <T16 : Out<out @Ann Number>> f16() {}

fun <T17 : Inv<in Inv<@Ann Number>>> f17() {}
fun <T18 : Inv<in In<@Ann Number>>> f18() {}
fun <T19 : Inv<in Out<@Ann Number>>> f19() {}
fun <T20 : Inv<out Inv<@Ann Number>>> f20() {}
fun <T21 : Inv<out In<@Ann Number>>> f21() {}
fun <T22 : Inv<out Out<@Ann Number>>> f22() {}

fun <T23 : Inv<in Inv<in @Ann Number>>> f23() {}
fun <T24 : Inv<in In<in @Ann Number>>> f24() {}
fun <T25 : Inv<in Out<out @Ann Number>>> f25() {}
fun <T26 : Inv<out Inv<out @Ann Number>>> f26() {}
fun <T27 : Inv<out In<in @Ann Number>>> f27() {}
fun <T28 : Inv<out Out<out @Ann Number>>> f28() {}

fun <T29 : Inv<@JvmSuppressWildcards @Ann Number>> f29() {}
fun <T30 : In<@JvmSuppressWildcards @Ann Number>> f30() {}
fun <T31 : Out<@JvmSuppressWildcards @Ann Number>> f31() {}

@JvmSuppressWildcards fun <T32 : Inv<@Ann Number>> f32() {}
@JvmSuppressWildcards fun <T33 : In<@Ann Number>> f33() {}
@JvmSuppressWildcards fun <T34 : Out<@Ann Number>> f34() {}

@JvmSuppressWildcards(true) fun <T35 : @JvmSuppressWildcards(false) Inv<@Ann Number>> f35() {}
@JvmSuppressWildcards(true) fun <T36 : @JvmSuppressWildcards(false) In<@Ann Number>> f36() {}
@JvmSuppressWildcards(true) fun <T37 : @JvmSuppressWildcards(false) Out<@Ann Number>> f37() {}

@JvmSuppressWildcards(false) fun <T38 : @JvmSuppressWildcards(true) Inv<@Ann Number>> f38() {}
@JvmSuppressWildcards(false) fun <T39 : @JvmSuppressWildcards(true) In<@Ann Number>> f39() {}
@JvmSuppressWildcards(false) fun <T40 : @JvmSuppressWildcards(true) Out<@Ann Number>> f40() {}

fun <T41 : Inv<@JvmWildcard @Ann Number>> f41() {}
fun <T42 : In<@JvmWildcard @Ann Number>> f42() {}
fun <T43 : Out<@JvmWildcard @Ann Number>> f43() {}

fun <T44 : Inv<In<@JvmWildcard @Ann Number>>> f44() {}
fun <T45 : Inv<Out<@JvmWildcard @Ann Number>>> f45() {}

@JvmSuppressWildcards fun <T46 : Inv<In<@JvmWildcard @Ann Number>>> f46() {}
@JvmSuppressWildcards fun <T47 : Inv<Out<@JvmWildcard @Ann Number>>> f47() {}
