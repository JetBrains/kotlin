enum class Color {
  RED,
  BLUE
}

fun box() = if(
     Color.valueOf("RED") == Color.RED
  && Color.valueOf("BLUE") == Color.BLUE
  && Color.values()[0] == Color.RED
  && Color.values()[1] == Color.BLUE
  ) "OK" else "fail"
