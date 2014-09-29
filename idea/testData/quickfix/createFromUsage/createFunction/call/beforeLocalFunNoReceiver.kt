// "Create function 'foo' from usage" "true"

fun test() {
    fun nestedTest(): Int {
        return <caret>foo(2, "2")
    }
}