// IGNORE_BACKEND_FIR: JVM_IR
enum class State {
  O,
  K
}

fun box() = "${State.O}${State.K}"
