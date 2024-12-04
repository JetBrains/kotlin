// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// script
fun foo(action: () -> Int) {
    action()
}

foo {
    42 + 42
}
