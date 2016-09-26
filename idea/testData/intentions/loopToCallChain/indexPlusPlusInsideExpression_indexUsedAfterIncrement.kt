// WITH_RUNTIME
//TODO: should not be available without "asSequence()"!
fun foo(list: List<String>): Int? {
    var index = 0
    <caret>for (s in list) {
        if (s.isBlank()) continue
        val x = s.length * index++
        if (x * 100 > index * index) return x
    }
    return null
}