fun foo() {}

fun test() {
    <caret>do /* aaa */ foo() // comment
    while(true)
}
