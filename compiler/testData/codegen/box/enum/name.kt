enum class State {
  O,
  K
}

fun box() = "${State.O.name()}${State.K.name()}"
