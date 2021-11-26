// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB
// SKIP_TXT

class A<T>
class B<T>(val x: MutableList<T>) : MutableList<T> by x

class C {
    operator fun <T> get(k: A<T>): T = TODO()
    operator fun <T> set(k: A<T>, v: T): Unit = TODO()
}

fun foo() {
    C()[A<B<Int>>()] += 2
}