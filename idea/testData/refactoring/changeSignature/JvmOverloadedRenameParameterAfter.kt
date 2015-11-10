@JvmOverloads fun <caret>foo(aa: Int, b: Int, c: Int = 1, d: Int = 2) {

}

fun test() {
    foo(aa = 1, b = 2, c = 3, d = 4)
    foo(aa = 1, b = 2, c = 3)
    foo(b = 1, c = 2, aa = 3)
    foo(aa = 1, b = 2)
    foo(b = 1, aa = 2)
}