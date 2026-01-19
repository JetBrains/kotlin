// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// WITH_STDLIB

fun foo() {
    try {

    } catch (_<caret>: Exception) {

    }
}
