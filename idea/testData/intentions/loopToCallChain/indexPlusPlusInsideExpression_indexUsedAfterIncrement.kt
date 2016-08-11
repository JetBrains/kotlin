// WITH_RUNTIME
//TODO: should not be available without "asSequence()"!
// INTENTION_TEXT: "Replace with 'filterNot{}.map{}.firstOrNull{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterNot{}.map{}.firstOrNull{}'"
fun foo(list: List<String>): Int? {
    var index = 0
    <caret>for (s in list) {
        if (s.isBlank()) continue
        val x = s.length * index++
        if (x * 100 > index * index) return x
    }
    return null
}