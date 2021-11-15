// !API_VERSION: 1.4
// WITH_STDLIB

val x = 0u
val y = 0uL

fun box(): String {
    if (x != 0u)  return "Fail 1"
    if (y != 0uL) return "Fail 2"
    return "OK"
}
