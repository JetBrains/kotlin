// WITH_RUNTIME
fun foo(list: List<Any?>): Int? {
    <caret>for (o in list) {
        if (o == null) continue
        val code = o.hashCode()
        return code
    }
    return null
}