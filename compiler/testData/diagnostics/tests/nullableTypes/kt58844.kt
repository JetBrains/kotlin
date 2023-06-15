// !RENDER_DIAGNOSTICS_MESSAGES

fun intFun(i: Int) {}
fun byteFun(i: Byte) {}

fun main(args: Array<String>) {
    var intVar: Int? = 1
    var byteVar: Byte? = 1

    intFun(<!TYPE_MISMATCH!>intVar?.toInt()<!>)
    byteFun(<!TYPE_MISMATCH!>byteVar?.toByte()<!>)
}
