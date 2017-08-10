// WITH_RUNTIME

package very.very.long.name.of.pckg

class Inv1<K>

fun <T : Any> foo1(<warning>receiver</warning>: Inv1<T>) {}
fun <K> test(c: Inv1<K>) {
    foo1(<error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T : Any> foo1(receiver: Inv1<T>): Unit
should be a subtype of: Any (declared upper bound T)
should be equal to: K (for parameter 'receiver')
">c</error>)
}

class Inv2<T, K>
fun <T : Any, K : Any> foo2(<warning>a</warning>: Inv2<T, K>) {}
fun <S, V> test(c: Inv2<S, V>) {
    foo2(<error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'K':
fun <T : Any, K : Any> foo2(a: Inv2<T, K>): Unit
should be a subtype of: Any (declared upper bound K)
should be equal to: V (for parameter 'a')
"><error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T : Any, K : Any> foo2(a: Inv2<T, K>): Unit
should be a subtype of: Any (declared upper bound T)
should be equal to: S (for parameter 'a')
">c</error></error>)
}



fun <K> subCallNullableUpperBound(): Inv1<K> = TODO()
fun <K : Any> subCallNullable(): Inv1<K?> = TODO()

fun <S> test() {
    foo1(<error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T : Any> foo1(receiver: Inv1<T>): Unit
should be a subtype of: Any (declared upper bound T)
should be equal to: S (for parameter 'receiver')
">subCallNullableUpperBound<S>()</error>)
    foo1(<error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T : Any> foo1(receiver: Inv1<T>): Unit
should be a subtype of: Any (declared upper bound T)
should be equal to: S? (for parameter 'receiver')
">subCallNullable<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'Any'">S</error>>()</error>)
}
