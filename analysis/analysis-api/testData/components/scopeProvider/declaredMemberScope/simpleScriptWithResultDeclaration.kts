// script
fun foo(action: () -> Int) {
    action()
}

foo {
    42 + 42
}
