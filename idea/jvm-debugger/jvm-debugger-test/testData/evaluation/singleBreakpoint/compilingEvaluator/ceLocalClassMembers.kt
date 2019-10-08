package ceLocalClassMembers

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




