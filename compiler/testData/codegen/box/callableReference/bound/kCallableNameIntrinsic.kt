// Enable when callable references to builtin members is supported
// IGNORE_BACKEND: JS
fun box(): String {
    var state = 0
    val name = (state++)::toString.name
    if (name != "toString") return "Fail 1: $name"
    if (state != 1) return "Fail 2: $state"

    return "OK"
}
