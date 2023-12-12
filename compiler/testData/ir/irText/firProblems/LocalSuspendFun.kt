// IGNORE_BACKEND_K2: ANY
//   Ignore reasons: anonymous suspend functions are prohibited, KT-62018
// ISSUE: KT-58332

fun foo() {
    val addNewStatusAction: suspend () -> Unit = useMemo {
        suspend fun() {

        }
    }
}

fun <T> useMemo(callback: () -> T): T {
    return callback()
}
