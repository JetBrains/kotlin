// !API_VERSION: 1.4
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val x = 0u
val y = 0uL

fun box(): String {
    if (x != 0u)  return "Fail 1"
    if (y != 0uL) return "Fail 2"
    return "OK"
}
