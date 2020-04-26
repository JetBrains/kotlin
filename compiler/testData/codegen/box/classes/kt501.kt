class Reluctant() {
     init {
        throw Exception("I'm not coming out")
     }
}

fun test1() : String {
  try {
      val b = Reluctant()
      return "Surprise!"
  }
  catch (ex : Exception) {
      return "I told you so"
  }
}


fun box() : String {
    if(test1() != "I told you so") return "test1 failed"

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
