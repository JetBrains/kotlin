// IGNORE_BACKEND_FIR: JVM_IR
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