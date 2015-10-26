abstract class MyList : List<String>

class ListImpl : J() {
    override val size: Int get() = super.size + 1
}

fun box(): String {
    val impl = ListImpl()
    if (impl.size != 56) return "fail 1"
    if (!impl.contains("abc")) return "fail 2"

    val l: List<String> = impl

    if (l.size != 56) return "fail 3"
    if (!l.contains("abc")) return "fail 4"

    val anyList: List<Any?> = impl as List<Any?>

    if (anyList.size != 56) return "fail 5"
    if (!anyList.contains("abc")) return "fail 6"

    if (anyList.contains(1)) return "fail 7"
    if (anyList.contains(null)) return "fail 8"

    return "OK"
}
