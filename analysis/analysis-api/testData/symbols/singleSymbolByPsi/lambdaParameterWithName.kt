// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

fun foo(action: (Int) -> Unit) {}

fun usage() {
    foo { ar<caret>g ->

    }
}
