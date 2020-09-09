// FIR_COMPARISON

class X<T> {
    class Nested {
        val v: <caret>
    }
}

// ABSENT: T
