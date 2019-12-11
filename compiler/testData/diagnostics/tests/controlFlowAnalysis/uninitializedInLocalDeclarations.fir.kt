fun test1() {
    fun bar() {
        var i : Int
        doSmth(i)
    }
}

fun test2() {
    fun foo() {
        val s: String?

        try {
            s = ""
        }
        catch(e: Exception) {
            doSmth(e)
        }

        doSmth(s)
    }
}

fun test3() {
    val f = {
        val a : Int
        doSmth(a)
    }
}

fun test4() {
    doSmth {
        val a : Int
        doSmth(a)
    }
}

fun test5() {
    fun inner1() {
        fun inner2() {
            fun inner3() {
                fun inner4() {
                    val a : Int
                    doSmth(a)
                }
            }
        }
    }
}

fun doSmth(a: Any?) = a