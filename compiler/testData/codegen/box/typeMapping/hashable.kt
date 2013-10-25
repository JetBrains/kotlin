fun foo(): Hashable = 1

fun box(): String {
    if (foo() == 1) return "OK"
    return "Fail"
}