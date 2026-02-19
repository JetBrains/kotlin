// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// WITH_STDLIB

package foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

interface Inv<T>
interface In<in U>
interface Out<out V>

private var f01: Inv<@Ann Number>? = null
private var f02: In<@Ann Number>? = null
private var f03: Out<@Ann Number>? = null

private var f04: Inv<Inv<@Ann Number>>? = null
private var f05: Inv<In<@Ann Number>>? = null
private var f06: Inv<Out<@Ann Number>>? = null
private var f07: In<Inv<@Ann Number>>? = null
private var f08: In<In<@Ann Number>>? = null
private var f09: In<Out<@Ann Number>>? = null
private var f10: Out<Inv<@Ann Number>>? = null
private var f11: Out<In<@Ann Number>>? = null
private var f12: Out<Out<@Ann Number>>? = null

private var f13: Inv<in @Ann Number>? = null
private var f14: Inv<out @Ann Number>? = null
private var f15: In<in @Ann Number>? = null
private var f16: Out<out @Ann Number>? = null

private var f17: Inv<in Inv<@Ann Number>>? = null
private var f18: Inv<in In<@Ann Number>>? = null
private var f19: Inv<in Out<@Ann Number>>? = null
private var f20: Inv<out Inv<@Ann Number>>? = null
private var f21: Inv<out In<@Ann Number>>? = null
private var f22: Inv<out Out<@Ann Number>>? = null

private var f23: Inv<in Inv<in @Ann Number>>? = null
private var f24: Inv<in In<in @Ann Number>>? = null
private var f25: Inv<in Out<out @Ann Number>>? = null
private var f26: Inv<out Inv<out @Ann Number>>? = null
private var f27: Inv<out In<in @Ann Number>>? = null
private var f28: Inv<out Out<out @Ann Number>>? = null

private var f29: Inv<@JvmSuppressWildcards @Ann Number>? = null
private var f30: In<@JvmSuppressWildcards @Ann Number>? = null
private var f31: Out<@JvmSuppressWildcards @Ann Number>? = null

@JvmSuppressWildcards private var f32: Inv<@Ann Number>? = null
@JvmSuppressWildcards private var f33: In<@Ann Number>? = null
@JvmSuppressWildcards private var f34: Out<@Ann Number>? = null

@JvmSuppressWildcards(true) private var f35: @JvmSuppressWildcards(false) Inv<@Ann Number>? = null
@JvmSuppressWildcards(true) private var f36: @JvmSuppressWildcards(false) In<@Ann Number>? = null
@JvmSuppressWildcards(true) private var f37: @JvmSuppressWildcards(false) Out<@Ann Number>? = null

@JvmSuppressWildcards(false) private var f38: @JvmSuppressWildcards(true) Inv<@Ann Number>? = null
@JvmSuppressWildcards(false) private var f39: @JvmSuppressWildcards(true) In<@Ann Number>? = null
@JvmSuppressWildcards(false) private var f40: @JvmSuppressWildcards(true) Out<@Ann Number>? = null

private var f41: Inv<@JvmWildcard @Ann Number>? = null
private var f42: In<@JvmWildcard @Ann Number>? = null
private var f43: Out<@JvmWildcard @Ann Number>? = null

private var f44: Inv<In<@JvmWildcard @Ann Number>>? = null
private var f45: Inv<Out<@JvmWildcard @Ann Number>>? = null

@JvmSuppressWildcards private var f46: Inv<In<@JvmWildcard @Ann Number>>? = null
@JvmSuppressWildcards private var f47: Inv<Out<@JvmWildcard @Ann Number>>? = null
