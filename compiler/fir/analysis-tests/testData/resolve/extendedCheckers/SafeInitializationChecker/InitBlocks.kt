// WITH_STDLIB

class Init(b: Boolean, w: Boolean, init: Init) {

    val a: String
    <!ACCESS_TO_UNINITIALIZED_VALUE, ACCESS_TO_UNINITIALIZED_VALUE!>var s: String<!>
    val t: Init

    fun foo(): String = s

    init {
        if (b) {
            if (w) {
                t = this
                s = ""
                a = foo()
            } else {
                a = foo()
                t = init
            }
            s = t.s
        } else {
            s = "ds"
            a = s
            t = this
        }
        s = a // checkEffs(), Promote(pots)
    }
}

class B {
    var b = "Hello"
    var a = foo()  // < < Доступ к неинициализированному полю

    fun foo(): String {
        a = b
        return a.substring(1)
    }
}

class B1 {
    var b = "Hello"
    var a: String

    init {
        a = b
        a = a.substring(1)
    }
}
