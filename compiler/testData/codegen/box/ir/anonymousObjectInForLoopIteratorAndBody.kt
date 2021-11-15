// WITH_STDLIB

fun box(): String {
    var result = ""
    for (x in listOf('O', 'A', 'K').filter { it > 'D' }) {
        result += object { fun run() = x }.run()
    }
    return result
}
