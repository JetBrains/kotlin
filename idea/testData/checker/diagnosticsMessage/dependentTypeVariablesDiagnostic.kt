fun <T : S, S : Number> foo(<warning>x</warning>: T, <warning>y</warning>: S) {}

fun test1(i: Int) {
    foo(i, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'S':
fun <T : S, S : Number> foo(x: T, y: S): Unit
should be a subtype of: Number (declared upper bound S)
should be a supertype of: Int (for parameter 'x'), String (for parameter 'y')
">""</error>)
}

fun test2(i: Int) {
    foo(<error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T : S, S : Number> foo(x: T, y: S): Unit
should be a subtype of: Number (declared upper bound S)
should be a supertype of: String (for parameter 'x')
">""</error>, i)
}

class Inv<T>
fun <T : S, S : K, K> bar(<warning>x</warning>: T, <warning>y</warning>: S, <warning>z</warning>: Inv<K>) {}

fun test3(inv: Inv<Double>) {
    bar("", "", <error descr="[TOO_MANY_ARGUMENTS] Too many arguments for public fun <T : S, S> bar(x: Inv<T>, y: Inv<S>): Unit defined in root package in file dependentTypeVariablesDiagnostic.kt">inv</error>)
}

fun <T : S, S> bar(<warning>x</warning>: Inv<T>, <warning>y</warning>: Inv<S>) {}

fun test4(a: Inv<Int>, b: Inv<String>) {
    bar(a, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'S':
fun <T : S, S> bar(x: Inv<T>, y: Inv<S>): Unit
should be equal to: String (for parameter 'y')
should be a supertype of: Int (for parameter 'x')
">b</error>)
}