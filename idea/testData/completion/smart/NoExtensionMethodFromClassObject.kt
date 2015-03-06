class X {
    default object {
        fun String.f(): X = X()
        fun g(): X = X()
    }
}

fun foo(): X {
    return <caret>
}

// ABSENT: f
// EXIST: { lookupString:"g", itemText:"X.g", tailText:"() (<root>)" }
