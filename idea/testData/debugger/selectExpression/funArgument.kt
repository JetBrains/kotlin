fun foo() {
    val a = 1
    bar(<caret>a)
}

fun bar(i: Int) = 1

// EXPECTED: a