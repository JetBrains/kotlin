// WITH_STDLIB
const val unresolved: Foo = "Hello"

fun test() {
    if (<expr>unresolved == "Hello"</expr>) {}
}