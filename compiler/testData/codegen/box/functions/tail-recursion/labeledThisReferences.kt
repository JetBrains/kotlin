class B {
    inner class C {
        tailRecursive fun h(counter : Int, x : Any) {
            if (counter > 0) {
                this@C.h(counter - 1, "tail")
            }
        }

        tailRecursive fun h2(x : Any) {
            this@B.h2("no recursion") // keep vigilance
        }

    }

    fun makeC() : C = C()

    fun h2(x : Any) {
    }
}

fun box() : String {
    B().makeC().h(1000000, "test")
    B().makeC().h2(0)
    return "OK"
}
