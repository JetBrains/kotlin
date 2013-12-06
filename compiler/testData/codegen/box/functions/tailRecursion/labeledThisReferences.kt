// !DIAGNOSTICS: -UNUSED_PARAMETER

class B {
    inner class C {
        tailRecursive fun h(counter : Int) {
            if (counter > 0) {
                this@C.h(counter - 1)
            }
        }

        <!NO_TAIL_CALLS_FOUND!>tailRecursive fun h2(x : Any)<!> {
            this@B.h2("no recursion") // keep vigilance
        }

    }

    fun makeC() : C = C()

    fun h2(x : Any) {
    }
}

fun box() : String {
    B().makeC().h(1000000)
    B().makeC().h2(0)
    return "OK"
}
