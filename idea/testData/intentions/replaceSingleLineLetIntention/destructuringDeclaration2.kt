// WITH_RUNTIME

fun test() {
    (1 to 2).let<caret> { (i, j) -> foo(1, 2) }
}

fun foo(i: Int, j: Int) = i + j