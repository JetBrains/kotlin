// !DIAGNOSTICS: -UNUSED_PARAMETER
tailRecursive fun Int.foo(x: Int) {
    return 1.foo(2)
}