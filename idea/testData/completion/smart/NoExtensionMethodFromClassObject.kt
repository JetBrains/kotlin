class X {
    class object {
        fun String.f(): X = X()
        fun g(): X = X()
    }
}

fun foo(): X {
    return <caret>
}

// ABSENT: X.f
// EXIST: X.g
