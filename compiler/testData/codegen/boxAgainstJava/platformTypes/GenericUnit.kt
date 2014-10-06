import GenericUnit.*

val key = Key<Unit>()

fun box(): String {
    val n1 = getNull(key)
    if (n1 != null) return "Fail 1: $n1"

    val n2 = get(key, null)
    if (n2 != null) return "Fail 2: $n2"

    val n3 = get(key, Unit)
    if (n3 == null) return "Fail 3.0: $n3"
    if (n3 != Unit) return "Fail 3.1: $n3"

    return "OK"
}