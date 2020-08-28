fun test() {
    foo2(<caret>::convert3)
}

fun foo2(convert: (Int, Int) -> Unit) {}

fun convert3(i: Int, j: Int, k: Int = 0) {}