// PROBLEM: none

class Foo {
    fun s() = ""

    fun test() {
        <caret>this.s()
    }
}