// DIAGNOSTICS: -UNUSED_PARAMETER

class Inv<T>

fun <K> foo(t: K?) = Inv<K>()

fun <T> bar(t: T) = foo(t)
fun <V1> bar1(t: V1): Inv<V1> = foo(t)
fun <V2> bar2(t: V2): Inv<V2> = foo(t)

fun <S> select(x: S, y: S): S = x

fun <T> fail(t: T?) = if (t == null) bar(t) else bar(t)
fun <F> fail1(t: F?, n: Nothing?) = select(bar1(n), bar2(t!!))
fun <F> fail2(t: F?, n: Nothing?) = if (t == null) bar1(t) else bar2(t)
fun <F> fail3(t: F?) = select(bar1(null), bar2(t?.let { it }))
