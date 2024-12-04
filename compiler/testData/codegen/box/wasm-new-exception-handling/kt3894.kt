// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved
class MyString {
    var s = ""
    operator fun plus(x : String) : MyString {
        s += x
        return this
    }

    override fun toString(): String {
        return s
    }
}

fun test1() : MyString {
    var r = MyString()
    while (true) {
      try {
          r + "Try"

          if (true) {
              r + "Break"
              break
          }

      } finally {
          return r + "Finally"
      }
    }
}



fun box(): String {
    if (test1().toString() != "TryBreakFinally") return "fail1: ${test1().toString()}"

    return "OK"
}