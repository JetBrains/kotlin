// WITH_RUNTIME
fun main() {
    <caret>for (i in 1..100) {
        if (i % 2 == 0) continue
        inner@
        for (j in 1..100) {
            continue@inner
        }
        for (j in 1..100) {
            continue
        }
    }
}