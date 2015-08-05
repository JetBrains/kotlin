enum class EE {
    A,
    B
}

fun foo(): EE {
    return E<caret>
}

// ORDER: valueOf
// ORDER: A
// ORDER: B
// ORDER: EE
