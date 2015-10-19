package test

class A {
    fun call() = inlineFun2 { stub() }

    internal inline fun inlineFun2(p: () -> Unit): String {
        p()

        return inlineFun {
            test()
        }
    }

    private fun stub() = "fail"

    private fun test() = "OK"


    inline internal fun inlineFun(p: () -> String): String {
        return p()
    }

}
