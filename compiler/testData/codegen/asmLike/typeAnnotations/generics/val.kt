// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// WITH_STDLIB

package foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

interface Inv<T>
interface In<in U>
interface Out<out V>

private val f01: Inv<@Ann Number>? = null
private val f02: In<@Ann Number>? = null
private val f03: Out<@Ann Number>? = null

private val f04: Inv<Inv<@Ann Number>>? = null
private val f05: Inv<In<@Ann Number>>? = null
private val f06: Inv<Out<@Ann Number>>? = null
private val f07: In<Inv<@Ann Number>>? = null
private val f08: In<In<@Ann Number>>? = null
private val f09: In<Out<@Ann Number>>? = null
private val f10: Out<Inv<@Ann Number>>? = null
private val f11: Out<In<@Ann Number>>? = null
private val f12: Out<Out<@Ann Number>>? = null

private val f13: Inv<in @Ann Number>? = null
private val f14: Inv<out @Ann Number>? = null
private val f15: In<in @Ann Number>? = null
private val f16: Out<out @Ann Number>? = null

private val f17: Inv<in Inv<@Ann Number>>? = null
private val f18: Inv<in In<@Ann Number>>? = null
private val f19: Inv<in Out<@Ann Number>>? = null
private val f20: Inv<out Inv<@Ann Number>>? = null
private val f21: Inv<out In<@Ann Number>>? = null
private val f22: Inv<out Out<@Ann Number>>? = null

private val f23: Inv<in Inv<in @Ann Number>>? = null
private val f24: Inv<in In<in @Ann Number>>? = null
private val f25: Inv<in Out<out @Ann Number>>? = null
private val f26: Inv<out Inv<out @Ann Number>>? = null
private val f27: Inv<out In<in @Ann Number>>? = null
private val f28: Inv<out Out<out @Ann Number>>? = null

private val f29: Inv<@JvmSuppressWildcards @Ann Number>? = null
private val f30: In<@JvmSuppressWildcards @Ann Number>? = null
private val f31: Out<@JvmSuppressWildcards @Ann Number>? = null

@JvmSuppressWildcards private val f32: Inv<@Ann Number>? = null
@JvmSuppressWildcards private val f33: In<@Ann Number>? = null
@JvmSuppressWildcards private val f34: Out<@Ann Number>? = null

@JvmSuppressWildcards(true) private val f35: @JvmSuppressWildcards(false) Inv<@Ann Number>? = null
@JvmSuppressWildcards(true) private val f36: @JvmSuppressWildcards(false) In<@Ann Number>? = null
@JvmSuppressWildcards(true) private val f37: @JvmSuppressWildcards(false) Out<@Ann Number>? = null

@JvmSuppressWildcards(false) private val f38: @JvmSuppressWildcards(true) Inv<@Ann Number>? = null
@JvmSuppressWildcards(false) private val f39: @JvmSuppressWildcards(true) In<@Ann Number>? = null
@JvmSuppressWildcards(false) private val f40: @JvmSuppressWildcards(true) Out<@Ann Number>? = null

private val f41: Inv<@JvmWildcard @Ann Number>? = null
private val f42: In<@JvmWildcard @Ann Number>? = null
private val f43: Out<@JvmWildcard @Ann Number>? = null

private val f44: Inv<In<@JvmWildcard @Ann Number>>? = null
private val f45: Inv<Out<@JvmWildcard @Ann Number>>? = null

@JvmSuppressWildcards private val f46: Inv<In<@JvmWildcard @Ann Number>>? = null
@JvmSuppressWildcards private val f47: Inv<Out<@JvmWildcard @Ann Number>>? = null
