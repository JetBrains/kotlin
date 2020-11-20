// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER -CAST_NEVER_SUCCEEDS
// ISSUE: KT-36819

// Case 1

fun <K> select(vararg x: K) = x[0]
interface A
class B: A
class C: A
fun <T> id1(x: T): T = x
fun <R> id2(x: R): R = x

class Out<out R>(x: R)

fun main() {
    val x1 = select(id1 { B() }, id2 { C() })
    val x2 = select({ B() }, { C() })
    val x3 = select(id1(Out(B())), id2(Out(C())))
}

// Case 2

fun <R> fold(initial: R, operation: (R) -> Unit) {}

fun foo() {
    fold({ x: Int -> x }) { acc -> }
}

// Case 3

class Foo

typealias X = Foo.(Foo.() -> Unit) -> Unit

fun bar() {
    val y = when {
        false -> { _ ->
            Unit
        }
        true -> null as X
        else -> { x ->
            x()
            Unit
        }
    }
}