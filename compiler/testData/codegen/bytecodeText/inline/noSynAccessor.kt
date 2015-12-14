inline fun call(s: () -> Unit) {
    s()
}

class A {

    private fun method() {}

    private val prop = 1

    fun test1() {
        call {
            method()
            prop
        }
    }

    fun test2() {
        call {
            call {
                method()
                prop
            }
        }
    }
}

//0 access\$