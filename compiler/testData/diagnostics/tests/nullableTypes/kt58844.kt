// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

fun intFun(i: Int) {}
fun byteFun(i: Byte) {}

fun main(args: Array<String>) {
    var intVar: Int? = 1
    var byteVar: Byte? = 1

    intFun(<!ARGUMENT_TYPE_MISMATCH("Int?; Int;  value parameter 'i' of;  'intVar'")!>intVar<!>)
    byteFun(<!ARGUMENT_TYPE_MISMATCH("Byte?; Byte;  value parameter 'i' of;  'byteVar'")!>byteVar<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
