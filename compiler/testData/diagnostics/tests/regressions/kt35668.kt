// DIAGNOSTICS: -UNUSED_PARAMETER

fun main() {
    val baseDir: String? = ""
    val networkParameters: String? = ""
    if (baseDir != null) {
        if (networkParameters != null) {
            Unit
        } else if (true){
            return
        } else {
            return
        }
    } else {
        return
    }

    <!DEBUG_INFO_SMARTCAST!>networkParameters<!>.length // unsafe call
}
