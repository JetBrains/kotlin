// LANGUAGE: +ExpectedUnitAsSoftConstraint
// ISSUE: KT-55168
fun foo(arg: Boolean) = buildList {
    if (arg) {
        removeLast()
    } else {
        add(42)
    }
}

fun bar(arg: Boolean) = buildList {
    if (!arg) {
        add(42)
    } else {
        removeLast()
    }
}
