// IGNORE_BACKEND_FIR: JVM_IR
//KT-3869 Loops and finally: outer finally block not run

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
        r + "Try"

        while(r.toString() != "") {
            return r + "Loop"
        }

        return r + "Fail"
    } finally {
        r + "Finally"
    }
}

fun test2() : MyString {
    var r = MyString()
    try {
        r + "Try"

        do {
            if (r.toString() != "") {
                return r + "Loop"
            }
        } while (r.toString() != "")

        return r + "Fail"
    } finally {
        r + "Finally"
    }
}

fun test3() : MyString {
    var r = MyString()
    try {
        r + "Try"

        for(i in 1..2) {
            r + "Loop"
            return r
        }

        return r + "Fail"
    } finally {
        r + "Finally"
    }
}

fun box(): String {
    if (test1().toString() != "TryLoopFinally") return "fail1: ${test1()}"
    if (test2().toString() != "TryLoopFinally") return "fail2: ${test2()}"
    if (test3().toString() != "TryLoopFinally") return "fail3: ${test3()}"

    return "OK"
}