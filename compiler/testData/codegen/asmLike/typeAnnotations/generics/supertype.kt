// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// WITH_STDLIB

package foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

interface Inv<T>
interface In<in U>
interface Out<out V>

class F01 : Inv<@Ann Number>
class F02 : In<@Ann Number>
class F03 : Out<@Ann Number>

class F04 : Inv<Inv<@Ann Number>>
class F05 : Inv<In<@Ann Number>>
class F06 : Inv<Out<@Ann Number>>
class F07 : In<Inv<@Ann Number>>
class F08 : In<In<@Ann Number>>
class F09 : In<Out<@Ann Number>>
class F10 : Out<Inv<@Ann Number>>
class F11 : Out<In<@Ann Number>>
class F12 : Out<Out<@Ann Number>>

class F17 : Inv<Inv<in Inv<@Ann Number>>>
class F18 : Inv<Inv<in In<@Ann Number>>>
class F19 : Inv<Inv<in Out<@Ann Number>>>
class F20 : Inv<Inv<out Inv<@Ann Number>>>
class F21 : Inv<Inv<out In<@Ann Number>>>
class F22 : Inv<Inv<out Out<@Ann Number>>>

class F23 : Inv<Inv<in @Ann Number>>
class F24 : Inv<In<in @Ann Number>>
class F25 : Inv<Out<out @Ann Number>>
class F26 : Inv<Inv<out @Ann Number>>
class F27 : Inv<In<in @Ann Number>>
class F28 : Inv<Out<out @Ann Number>>

class F29 : Inv<@JvmSuppressWildcards @Ann Number>
class F30 : In<@JvmSuppressWildcards @Ann Number>
class F31 : Out<@JvmSuppressWildcards @Ann Number>

@JvmSuppressWildcards class F32 : Inv<@Ann Number>
@JvmSuppressWildcards class F33 : In<@Ann Number>
@JvmSuppressWildcards class F34 : Out<@Ann Number>

@JvmSuppressWildcards(true) class F35 : @JvmSuppressWildcards(false) Inv<@Ann Number>
@JvmSuppressWildcards(true) class F36 : @JvmSuppressWildcards(false) In<@Ann Number>
@JvmSuppressWildcards(true) class F37 : @JvmSuppressWildcards(false) Out<@Ann Number>

@JvmSuppressWildcards(false) class F38 : @JvmSuppressWildcards(true) Inv<@Ann Number>
@JvmSuppressWildcards(false) class F39 : @JvmSuppressWildcards(true) In<@Ann Number>
@JvmSuppressWildcards(false) class F40 : @JvmSuppressWildcards(true) Out<@Ann Number>

class F41 : Inv<@JvmWildcard @Ann Number>
class F42 : In<@JvmWildcard @Ann Number>
class F43 : Out<@JvmWildcard @Ann Number>

class F44 : Inv<In<@JvmWildcard @Ann Number>>
class F45 : Inv<Out<@JvmWildcard @Ann Number>>

@JvmSuppressWildcards class F46 : Inv<In<@JvmWildcard @Ann Number>>
@JvmSuppressWildcards class F47 : Inv<Out<@JvmWildcard @Ann Number>>
