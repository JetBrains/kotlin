// FIR_COMPARISON

fun Some.first() {
}

class Some() {
}

fun Some.second() {
    this.<caret>
}

// EXIST: first, second