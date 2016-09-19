val paramTest = 12

fun small(paramFirst: Sequence<String>, paramSecond: Comparable<kotlin.collections.List<kotlin.Any>>) {
}

fun test() = small(<caret>)

// EXIST: {"lookupString":"paramSecond","tailText":" Comparable<List<Any>>","itemText":"paramSecond ="}
// EXIST: {"lookupString":"paramFirst","tailText":" Sequence<String>","itemText":"paramFirst ="}
