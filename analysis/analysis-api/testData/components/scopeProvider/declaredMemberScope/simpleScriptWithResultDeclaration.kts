// DO_NOT_CHECK_SYMBOL_RESTORE
// script
fun foo(action: () -> Int) {
    action()
}

foo {
    42 + 42
}
