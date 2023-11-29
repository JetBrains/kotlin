// WITH_STDLIB
// ISSUE: KT-51143

fun main() {
    <!NEW_INFERENCE_ERROR!>buildMap {
        if (true) {
            println("test")
        } else {
            put("foo", "bar")
        }
    }<!>
}
