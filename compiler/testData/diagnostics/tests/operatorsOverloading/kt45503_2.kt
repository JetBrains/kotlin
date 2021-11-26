// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

class A<T>(var x: T)

interface I

class C {
    operator fun <T> get(k: A<T>): T = k.x
    operator fun <T : I> set(k: A<T>, v: T) { k.x = v }
}

fun foo() {
    C()[A(mutableListOf(1))] += 2
}