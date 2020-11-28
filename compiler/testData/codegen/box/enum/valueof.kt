// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
enum class Color {
  RED,
  BLUE
}

fun throwsOnGreen(): Boolean {
    try {
        Color.valueOf("GREEN")
        return false
    }
    catch (e: Exception) {
        return true
    }
}

fun box() = if(
     Color.valueOf("RED") == Color.RED
  && Color.valueOf("BLUE") == Color.BLUE
  && Color.values()[0] == Color.RED
  && Color.values()[1] == Color.BLUE
  && throwsOnGreen()
  ) "OK" else "fail"