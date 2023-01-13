// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class E(val b: Boolean) {
    TRUE(1 == 1)
}

fun box() = if (E.TRUE.b) "OK" else "fail"