fun foo(o: Any) {
    if (o !is String) return
    val l = o.length
    somethingElse(o as String/* we should not remove this cast because it's not in pasted range*/)
}