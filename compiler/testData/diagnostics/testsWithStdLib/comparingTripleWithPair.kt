// ISSUE: KT-47979

enum class Foo { A, B }

fun test() {
    if (<!EQUALITY_NOT_APPLICABLE!>Triple(Foo.A, 1, 2) == Pair("a", "b")<!>) println("Doesn't compile")
    if (<!EQUALITY_NOT_APPLICABLE!>Triple(0, 1, 2) == Pair(Foo.A, "a")<!>) println("Doesn't compile")
    if (<!EQUALITY_NOT_APPLICABLE!>Triple(0, 1, 2) == Pair("a", "b")<!>) println("Doesn't compile")
    if (Triple(Foo.A, 1, 2) == Pair(Foo.A, "a")) println("Compiles, but why?")
}
