package test

private val prop = "O"

private fun test() = "K"

inline internal fun inlineFun(): String {
    return prop + test()
}

class A () {
    fun call() = inlineFun()
}

