// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES

fun intFun(i: Int) {}
fun byteFun(i: Byte) {}

fun main(args: Array<String>) {
    var intVar: Int? = 1
    var byteVar: Byte? = 1

    intFun(<!ARGUMENT_TYPE_MISMATCH("Int?; Int")!>intVar?.toInt()<!>)
    byteFun(<!ARGUMENT_TYPE_MISMATCH("Byte?; Byte")!>byteVar?.toByte()<!>)
}
