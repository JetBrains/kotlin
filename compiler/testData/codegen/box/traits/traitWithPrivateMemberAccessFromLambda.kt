interface Z {

    fun testFun(): String {
        return { privateFun() }.let { it() }
    }

    fun testProperty(): String {
        return { privateProp }.let { it() }
    }

    private fun privateFun(): String {
        return "O"
    }

    private val privateProp: String
        get() = "K"
}

object Z2 : Z {

}

fun box(): String {
    return Z2.testFun() + Z2.testProperty()
}

