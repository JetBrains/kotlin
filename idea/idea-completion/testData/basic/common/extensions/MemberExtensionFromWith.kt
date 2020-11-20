// FIR_COMPARISON
class X {
    fun String.f() {}
}

fun fn() {
    with (X()) {
        "sss".<caret>
    }
}

// EXIST: f
