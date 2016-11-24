// !DIAGNOSTICS: -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_VARIABLE
class GenericController<T> {
    operator fun handleResult(x: T, c: Continuation<Nothing>) { }
    suspend fun <V> await(f: V): V = f
}

fun <T> genericBuilder(coroutine c: GenericController<T>.() -> Continuation<Unit>): T = null!!

fun foo() {
    var result = ""
    genericBuilder<String> {
        try {
            await("")
        } catch(e: Exception) {
            <!EXPECTED_TYPE_MISMATCH!>result = "fail"<!>
        }
    }
}
