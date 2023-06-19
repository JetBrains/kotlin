fun test() {
    with("Hello, world!") str@{
        <caret>val x = 0
    }
}