// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class State {
  O,
  K
}

fun box() = "${State.O}${State.K}"
