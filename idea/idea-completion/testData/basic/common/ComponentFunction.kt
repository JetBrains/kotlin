data class X {
    operator fun component1(): Int = 0
}

fun foo(x: X) {
    x.<caret>
}

// EXIST: component1
