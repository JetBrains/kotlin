data class Data(val aaa: Int)

fun foo(d: Data) {
    d.equ<caret>als(d)
}