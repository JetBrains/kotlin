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