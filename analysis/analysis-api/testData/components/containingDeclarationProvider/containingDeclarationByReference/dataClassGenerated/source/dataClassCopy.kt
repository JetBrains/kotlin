data class Data(val aaa: Int)

fun foo(d: Data) {
    d.co<caret>py(aaa = 1)
}