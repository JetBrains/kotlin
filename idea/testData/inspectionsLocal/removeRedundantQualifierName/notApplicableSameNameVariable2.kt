// PROBLEM: none
// WITH_RUNTIME
sealed class Foo {
    object BAR : Foo()

    companion object {
        val BAR: Foo by lazy { <caret>Foo.BAR }
    }
}