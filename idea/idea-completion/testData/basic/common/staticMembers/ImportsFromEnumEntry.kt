package b

import b.Bar.A.<caret>


enum class Bar {
    A;

    fun foo() {}
}

// INVOCATION_COUNT: 2
// ABSENT: foo