object O {
    private val zzzz = 0
}

fun foo() {
    O.z<caret>
}

// INVOCATION_COUNT: 0
// NUMBER: 0
