// TARGET_BACKEND: JVM_IR

fun foo(x: MutableList<String>) {
    x.addFirst("1")
    x.addLast("2")
}

fun bar(x: MutableList<String>) {
    x.removeFirst()
    x.removeLast()
}

fun box(): String {
    val list = mutableListOf("OK")
    foo(list)
    if (list.first() != "1") return "FAIL 1"
    if (list.last() != "2") return "FAIL 2"
    bar(list)
    return list.single()
}
