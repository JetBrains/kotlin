package source

class X {
    companion object {


        fun other() {
            foo()
        }
    }

    fun f() {
        bar++
    }
}


fun foo() {
    X.other()
    bar++
}

var bar = 1

