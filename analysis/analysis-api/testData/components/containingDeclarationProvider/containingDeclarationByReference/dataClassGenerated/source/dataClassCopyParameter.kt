data class Data(val aaa: Int)

fun foo(d: Data) {
    d.copy(aa<caret>a = 1)
}