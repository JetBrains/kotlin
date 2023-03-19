enum class En {
    A,
    B
}

fun box(): String {

  val u: Unit = when(En.A) {
    En.A -> {}
    En.B -> {}
  }

  return "OK"
}
