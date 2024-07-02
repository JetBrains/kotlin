// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

@NameBased
data class Foo(val o: String, val k: String)

fun box(): String {
    val (k, o, _) = Foo("O", "K")
    return o + k
}