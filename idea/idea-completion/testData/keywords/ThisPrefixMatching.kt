// FIR_COMPARISON
fun is1(): Boolean{}
fun is2(): Boolean{}

class Outer {
    inner class Inner {
        fun String.foo() {
            is<caret>
        }
    }
}

// NUMBER: 0
