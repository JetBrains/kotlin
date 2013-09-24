trait SuperTrait {
    public fun toString(): String = "!"
}

data class A(val x: Int): SuperTrait {
}

fun box(): String {
  return if (A(0).toString() == "!") "OK" else "fail"
}