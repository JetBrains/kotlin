// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

@NameBased
data class Foo(val o: String, val k: String)

fun foo(foos: List<Foo>): String {
    var result = ""
    for ((k, o) in foos) {
        result = o + k
    }
    return result
}

fun box(): String {
    return foo(listOf(Foo("O", "K")))
}