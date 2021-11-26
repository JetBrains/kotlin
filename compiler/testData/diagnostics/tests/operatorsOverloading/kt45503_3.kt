// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB
// SKIP_TXT

class A<T>
class C
interface I

class E {
    operator fun <T> get(k: A<T>): T = TODO()
    operator fun <T : I> set(k: A<T>, v: T) { TODO() }
    operator fun set(k: A<C>, v: C) { TODO() }
}

fun foo() {
    E()[A<MutableList<Int>>()] += 1
}