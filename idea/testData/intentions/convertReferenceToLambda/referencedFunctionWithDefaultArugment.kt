fun test() {
    foo1(<caret>::convert2)
}

fun foo1(convert: (Int) -> Unit) {}

fun convert2(i: Int, j: Int = 0) {}