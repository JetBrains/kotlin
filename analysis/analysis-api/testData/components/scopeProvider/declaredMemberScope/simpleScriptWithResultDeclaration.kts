// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// script
fun foo(action: () -> Int) {
    action()
}

foo {
    42 + 42
}
