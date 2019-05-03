fun foo(b: Boolean) {
    <caret>if (b) bar(1) // comment1
    else bar(2) // comment2
}

fun bar(i: Int) {}