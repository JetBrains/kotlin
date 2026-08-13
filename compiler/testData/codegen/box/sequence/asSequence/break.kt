// WITH_STDLIB

fun box(): String {
    val list = listOf(1, 2, 3)
    val seq = list.asSequence()
    label@ for (j in 1..2) {
        for (i in seq.map { it * 2 }) {
            break@label
        }
    }
    return "OK"
}