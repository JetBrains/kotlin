// !LANGUAGE: +ContextReceivers

interface A
interface B
interface C
interface D

<!CONFLICTING_OVERLOADS!>context(A, B)
fun f(): Unit<!> = TODO()

<!CONFLICTING_OVERLOADS!>context(C, D)
fun f(): Unit<!> = TODO()

fun test(a: A, b: B) {
    with(a) {
        with(b) {
            f()
        }
    }
}
