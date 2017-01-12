fun isNull(x: Unit?) = x == null

fun box(): String {
    val closure: () -> Unit? = { null }
    if (!isNull(closure())) return "Fail 1"

    return "OK"
}
