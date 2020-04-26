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

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNIT
