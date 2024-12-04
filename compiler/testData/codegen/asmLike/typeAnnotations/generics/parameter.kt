// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// WITH_STDLIB

package foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

interface Inv<T>
interface In<in U>
interface Out<out V>

fun f01(p: Inv<@Ann Number>) {}
fun f02(p: In<@Ann Number>) {}
fun f03(p: Out<@Ann Number>) {}

fun f04(p: Inv<Inv<@Ann Number>>) {}
fun f05(p: Inv<In<@Ann Number>>) {}
fun f06(p: Inv<Out<@Ann Number>>) {}
fun f07(p: In<Inv<@Ann Number>>) {}
fun f08(p: In<In<@Ann Number>>) {}
fun f09(p: In<Out<@Ann Number>>) {}
fun f10(p: Out<Inv<@Ann Number>>) {}
fun f11(p: Out<In<@Ann Number>>) {}
fun f12(p: Out<Out<@Ann Number>>) {}

fun f13(p: Inv<in @Ann Number>) {}
fun f14(p: Inv<out @Ann Number>) {}
fun f15(p: In<in @Ann Number>) {}
fun f16(p: Out<out @Ann Number>) {}

fun f17(p: Inv<in Inv<@Ann Number>>) {}
fun f18(p: Inv<in In<@Ann Number>>) {}
fun f19(p: Inv<in Out<@Ann Number>>) {}
fun f20(p: Inv<out Inv<@Ann Number>>) {}
fun f21(p: Inv<out In<@Ann Number>>) {}
fun f22(p: Inv<out Out<@Ann Number>>) {}

fun f23(p: Inv<in Inv<in @Ann Number>>) {}
fun f24(p: Inv<in In<in @Ann Number>>) {}
fun f25(p: Inv<in Out<out @Ann Number>>) {}
fun f26(p: Inv<out Inv<out @Ann Number>>) {}
fun f27(p: Inv<out In<in @Ann Number>>) {}
fun f28(p: Inv<out Out<out @Ann Number>>) {}

fun f29(p: Inv<@JvmSuppressWildcards @Ann Number>) {}
fun f30(p: In<@JvmSuppressWildcards @Ann Number>) {}
fun f31(p: Out<@JvmSuppressWildcards @Ann Number>) {}

@JvmSuppressWildcards fun f32(p: Inv<@Ann Number>) {}
@JvmSuppressWildcards fun f33(p: In<@Ann Number>) {}
@JvmSuppressWildcards fun f34(p: Out<@Ann Number>) {}

@JvmSuppressWildcards(true) fun f35(p: @JvmSuppressWildcards(false) Inv<@Ann Number>) {}
@JvmSuppressWildcards(true) fun f36(p: @JvmSuppressWildcards(false) In<@Ann Number>) {}
@JvmSuppressWildcards(true) fun f37(p: @JvmSuppressWildcards(false) Out<@Ann Number>) {}

@JvmSuppressWildcards(false) fun f38(p: @JvmSuppressWildcards(true) Inv<@Ann Number>) {}
@JvmSuppressWildcards(false) fun f39(p: @JvmSuppressWildcards(true) In<@Ann Number>) {}
@JvmSuppressWildcards(false) fun f40(p: @JvmSuppressWildcards(true) Out<@Ann Number>) {}

fun f41(p: Inv<@JvmWildcard @Ann Number>) {}
fun f42(p: In<@JvmWildcard @Ann Number>) {}
fun f43(p: Out<@JvmWildcard @Ann Number>) {}

fun f44(p: Inv<In<@JvmWildcard @Ann Number>>) {}
fun f45(p: Inv<Out<@JvmWildcard @Ann Number>>) {}

@JvmSuppressWildcards fun f46(p: Inv<In<@JvmWildcard @Ann Number>>) {}
@JvmSuppressWildcards fun f47(p: Inv<Out<@JvmWildcard @Ann Number>>) {}
