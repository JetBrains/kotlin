class A {
    public val publicVal: Int = 1
    protected val protectedVal: Int = 2
    private val privateVal: Int = 3

    public fun publicFun(): Int = 4
    private fun privateFun() = 5
}

val p: Int = A().p<caret>

// EXIST: publicVal
// ABSENT: protectedVal
// ABSENT: privateVal
// EXIST: publicFun
// ABSENT: privateFun
