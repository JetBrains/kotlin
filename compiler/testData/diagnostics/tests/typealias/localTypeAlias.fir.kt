// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun <T> emptyList(): List<T> = null!!

fun <T> foo() {
    typealias LT = List<T>

    val a: <!UNRESOLVED_REFERENCE!>LT<!> = emptyList()

    fun localFun(): <!UNRESOLVED_REFERENCE!>LT<!> {
        typealias LLT = List<T>

        val b: <!UNRESOLVED_REFERENCE!>LLT<!> = a

        return b
    }

    localFun()
}
