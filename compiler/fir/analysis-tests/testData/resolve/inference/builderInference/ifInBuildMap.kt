// WITH_STDLIB
// ISSUE: KT-51143

fun main() {
    buildMap {
        if (true) {
            println("test")
        } else {
            put("foo", "bar")
        }
    }
}
