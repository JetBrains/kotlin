// "Create function 'foo'" "true"

fun test() {
    fun nestedTest(): Int {
        return <caret>foo(2, "2")
    }
}