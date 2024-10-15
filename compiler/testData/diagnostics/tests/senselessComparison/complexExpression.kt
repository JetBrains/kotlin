// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT

fun String?.repro(): Boolean {
    return this?.let {
        return false
    } == true
}