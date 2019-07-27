// OUT_OF_CODE_BLOCK: FALSE

class B(val a: A)
val B.foo: Int
    get() {
        return <caret>when (a) {
            A.e1 -> 1
            A.e2 -> 4
        }
    }