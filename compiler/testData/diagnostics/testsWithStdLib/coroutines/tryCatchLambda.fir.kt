// !DIAGNOSTICS: -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

suspend fun <V> await(f: V): V = f

fun <T> genericBuilder(c: suspend () -> T): T = null!!

fun foo() {
    var result = ""
    genericBuilder<String> {
        <!ARGUMENT_TYPE_MISMATCH!>try {
            await("")
        } catch(e: Exception) {
            result = "fail"
        }<!>
    }
}
