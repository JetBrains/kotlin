class X {
    fun String.f() {}
}

fun foo() {
    with (X()) {
        <caret>
    }
}

// ABSENT: f
