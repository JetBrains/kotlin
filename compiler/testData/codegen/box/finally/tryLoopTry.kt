// IGNORE_BACKEND_FIR: JVM_IR
//test for appropriate

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
    try {
        r + "Try1"
        for(i in 1..1) {
            try {
                r + "Try2"
            } finally {
                return r + "Finally2"
            }
        }

    } finally {
        r + "Finally1"
    }
    return r  + "Fail"
}

fun box(): String {
  return if (test1().toString() == "Try1Try2Finally2Finally1") "OK" else "fail: ${test1()}"
}