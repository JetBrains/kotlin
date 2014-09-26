// KT-4234

fun box(): String {
    class C

    val name = javaClass<C>().getSimpleName()
    if (name != "box\$C") return "Fail: $name"

    return "OK"
}
