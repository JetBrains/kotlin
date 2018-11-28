fun foo(list: List<String>): Int {
    <caret>val first = list.firstOrNull()
    if (first == null) return -1
    return first.length
}