// ISSUE: KT-52934

suspend fun <R> suspendRun(block: suspend () -> R): R = null!!

suspend fun test() {
    var e: Throwable? = null
    try {
        suspendRun {
            if (true) {
                <!RETURN_NOT_ALLOWED!>return<!>
            }
        }
    } finally {
        if (e != null) {
            throw e
        }
    }
}
