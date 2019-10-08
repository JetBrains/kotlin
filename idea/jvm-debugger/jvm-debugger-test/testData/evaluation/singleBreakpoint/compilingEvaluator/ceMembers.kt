package ceMembers

fun main(args: Array<String>) {
    A().test()
}

class A {
    public fun publicFun(): Int = 1
    public val publicVal: Int = 1

    protected fun protectedFun(): Int = 1
    protected val protectedVal: Int = 1

    private fun privateFun() = 1
    private val privateVal = 1

    fun test() {
        //Breakpoint!
        val a = 1
    }
}

fun foo(p: () -> Int) = p()

// EXPRESSION: foo { publicFun() }
// RESULT: 1: I

// EXPRESSION: foo { publicVal }
// RESULT: 1: I

// EXPRESSION: foo { protectedFun() }
// RESULT: 1: I

// EXPRESSION: foo { protectedVal }
// RESULT: 1: I

// EXPRESSION: foo { privateFun() }
// RESULT: 1: I

// EXPRESSION: foo { privateVal }
// RESULT: 1: I

