// PROBLEM: none
// FIX: none
enum class Foo : Bar {
    ONE;

    val double = x<caret>
}

interface Bar {
    val x: Int get() = 42
}