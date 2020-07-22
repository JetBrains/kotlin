// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun <T> emptyList(): List<T> = null!!

fun <T> foo() {
    typealias LT = List<T>

    val a: <!OTHER_ERROR, OTHER_ERROR!>LT<!> = emptyList()

    fun localFun(): <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>LT<!> {
        typealias LLT = List<T>

        val b: <!OTHER_ERROR, OTHER_ERROR!>LLT<!> = a

        return b
    }

    localFun()
}
