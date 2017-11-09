// !DIAGNOSTICS: -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_VARIABLE
suspend fun <V> await(f: V): V = f

fun <T> genericBuilder(c: suspend () -> T): T = null!!

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
