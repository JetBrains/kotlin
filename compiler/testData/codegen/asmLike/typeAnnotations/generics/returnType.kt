// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// WITH_STDLIB

package foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

interface Inv<T>
interface In<in U>
interface Out<out V>

fun f01(): Inv<@Ann Number>? = null
fun f02(): In<@Ann Number>? = null
fun f03(): Out<@Ann Number>? = null

fun f04(): Inv<Inv<@Ann Number>>? = null
fun f05(): Inv<In<@Ann Number>>? = null
fun f06(): Inv<Out<@Ann Number>>? = null
fun f07(): In<Inv<@Ann Number>>? = null
fun f08(): In<In<@Ann Number>>? = null
fun f09(): In<Out<@Ann Number>>? = null
fun f10(): Out<Inv<@Ann Number>>? = null
fun f11(): Out<In<@Ann Number>>? = null
fun f12(): Out<Out<@Ann Number>>? = null

fun f13(): Inv<in @Ann Number>? = null
fun f14(): Inv<out @Ann Number>? = null
fun f15(): In<in @Ann Number>? = null
fun f16(): Out<out @Ann Number>? = null

fun f17(): Inv<in Inv<@Ann Number>>? = null
fun f18(): Inv<in In<@Ann Number>>? = null
fun f19(): Inv<in Out<@Ann Number>>? = null
fun f20(): Inv<out Inv<@Ann Number>>? = null
fun f21(): Inv<out In<@Ann Number>>? = null
fun f22(): Inv<out Out<@Ann Number>>? = null

fun f23(): Inv<in Inv<in @Ann Number>>? = null
fun f24(): Inv<in In<in @Ann Number>>? = null
fun f25(): Inv<in Out<out @Ann Number>>? = null
fun f26(): Inv<out Inv<out @Ann Number>>? = null
fun f27(): Inv<out In<in @Ann Number>>? = null
fun f28(): Inv<out Out<out @Ann Number>>? = null

fun f29(): Inv<@JvmSuppressWildcards @Ann Number>? = null
fun f30(): In<@JvmSuppressWildcards @Ann Number>? = null
fun f31(): Out<@JvmSuppressWildcards @Ann Number>? = null

@JvmSuppressWildcards fun f32(): Inv<@Ann Number>? = null
@JvmSuppressWildcards fun f33(): In<@Ann Number>? = null
@JvmSuppressWildcards fun f34(): Out<@Ann Number>? = null

@JvmSuppressWildcards(true) fun f35(): @JvmSuppressWildcards(false) Inv<@Ann Number>? = null
@JvmSuppressWildcards(true) fun f36(): @JvmSuppressWildcards(false) In<@Ann Number>? = null
@JvmSuppressWildcards(true) fun f37(): @JvmSuppressWildcards(false) Out<@Ann Number>? = null

@JvmSuppressWildcards(false) fun f38(): @JvmSuppressWildcards(true) Inv<@Ann Number>? = null
@JvmSuppressWildcards(false) fun f39(): @JvmSuppressWildcards(true) In<@Ann Number>? = null
@JvmSuppressWildcards(false) fun f40(): @JvmSuppressWildcards(true) Out<@Ann Number>? = null

fun f41(): Inv<@JvmWildcard @Ann Number>? = null
fun f42(): In<@JvmWildcard @Ann Number>? = null
fun f43(): Out<@JvmWildcard @Ann Number>? = null

fun f44(): Inv<In<@JvmWildcard @Ann Number>>? = null
fun f45(): Inv<Out<@JvmWildcard @Ann Number>>? = null

@JvmSuppressWildcards fun f46(): Inv<In<@JvmWildcard @Ann Number>>? = null
@JvmSuppressWildcards fun f47(): Inv<Out<@JvmWildcard @Ann Number>>? = null
