fun foo(): Boolean {
    return true
}

fun main() {
    <caret>if (foo()) {
        //TODO
    }
}