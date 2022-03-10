class Test {
    val p = <expr>42 / d</expr> // uninitialized d
    val d = 0 // should not raise div-by-zero error
}

