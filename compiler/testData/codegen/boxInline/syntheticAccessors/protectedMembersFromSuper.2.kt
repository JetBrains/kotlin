package test

open class Base {
    protected open val FOO = "O"

    protected open fun test() = "K"
}

open class P : Base() {

    inline fun protectedProp(): String {
        return FOO
    }

    inline fun protectedFun(): String {
        return test()
    }
}

