// LANGUAGE_VERSION: 1.2
// PROBLEM: none

fun test() {
    lateinit <caret>var foo: String
    foo = "a"
}
