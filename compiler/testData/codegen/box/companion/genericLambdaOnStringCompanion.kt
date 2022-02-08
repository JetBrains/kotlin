// WITH_STDLIB

fun <T> T.f(E: (y: T) -> String): Boolean = E(this).isEmpty()

fun fu1() = (String).f { v -> "" }

fun box(): String {
    if (!fu1()) return "Failed: Expect lambda to return empty string"
    return "OK"
}