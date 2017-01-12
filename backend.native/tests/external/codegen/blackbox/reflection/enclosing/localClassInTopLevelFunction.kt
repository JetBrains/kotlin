// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT
// KT-4234

fun box(): String {
    class C

    val name = C::class.java.getSimpleName()
    if (name != "box\$C") return "Fail: $name"

    return "OK"
}
