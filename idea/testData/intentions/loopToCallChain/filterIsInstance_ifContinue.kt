// WITH_RUNTIME
fun foo(list: List<Any>): Int? {
    <caret>for (o in list) {
        if (o !is String) continue
        val length = o.length
        return length
    }
    return null
}