// OUT_OF_CODE_BLOCK: FALSE
// ERROR: Unresolved reference: awhen
enum class A {
    e1, e2, e3
}

class B(val a: A)

val B.foo: Int
    get() {
        return <caret>when (a) {
            A.e1 -> 1
            A.e2 -> 4
        }
    }