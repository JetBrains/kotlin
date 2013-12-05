tailRecursive fun foo() {
    return if (true) {
        (foo())
    }
    else Unit.VALUE
}