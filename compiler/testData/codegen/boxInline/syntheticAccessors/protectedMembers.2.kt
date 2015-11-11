package test

open class P {
    protected open val FOO = "O"

    protected open fun test() = "K"

    inline fun protectedProp(): String {
        return FOO
    }

    inline fun protectedFun(): String {
        return test()
    }
}

