// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS

open class Expression<K>

class ModOp<T : Number?, S : Number?>(
    val expr1: Expression<T>,
    val expr2: Expression<S>
)

class QueryParameter<A> : Expression<A>()

fun <T, S : T?> Expression<in S>.wrap(value: T): QueryParameter<T> = null as QueryParameter<T>

fun <T : Number?, S : T> Expression<T>.rem(t: S): ModOp<T, S> = ModOp(this, wrap(t))
