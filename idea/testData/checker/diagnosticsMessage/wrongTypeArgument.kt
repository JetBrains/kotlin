// WITH_RUNTIME

class Inv1<K>
fun <T> foo1(<warning>a</warning>: Inv1<T>, <warning>b</warning>: T) {}

fun <S, P> test1(a: Inv1<S>, b: P) {
    foo1(a, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> foo1(a: Inv1<T>, b: T): Unit
should be equal to: S (for parameter 'a')
should be a supertype of: P (for parameter 'b')
">b</error>)
}

fun <S> test2(a: Inv1<S>, b: S?) {
    foo1(a, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> foo1(a: Inv1<T>, b: T): Unit
should be equal to: S (for parameter 'a')
should be a supertype of: S? (for parameter 'b')
">b</error>)
}

fun <T> foo2(<warning>a</warning>: T, <warning>b</warning>: Inv1<T>) {}

fun <S, P> test3(a: S, b: Inv1<P>) {
    foo2(a, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> foo2(a: T, b: Inv1<T>): Unit
should be equal to: P (for parameter 'b')
should be a supertype of: S (for parameter 'a')
">b</error>)
}

fun <S> subCall(): S = TODO()

fun <K, S> test4() {
    foo1(subCall<Inv1<K>>(), <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> foo1(a: Inv1<T>, b: T): Unit
should be equal to: K (for parameter 'a')
should be a supertype of: S (for parameter 'b')
">subCall<S>()</error>)
}

class Inv2<K, V>
fun <K, V> foo3(<warning descr="[UNUSED_PARAMETER] Parameter 'a' is never used">a</warning>: Inv2<K, V>, <warning descr="[UNUSED_PARAMETER] Parameter 'key' is never used">key</warning>: K) {}

fun <T, S> test(a: Inv2<T, S>, v: S) {
    foo3(a, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'K':
fun <K, V> foo3(a: Inv2<K, V>, key: K): Unit
should be equal to: T (for parameter 'a')
should be a supertype of: S (for parameter 'key')
">v</error>)
}