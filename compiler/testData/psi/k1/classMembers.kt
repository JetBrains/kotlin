package test

abstract class ClassMembers(private val p: Int, public open var p2: String, p3: Int, p4: Int = 10, final val p5: String = "aaa") {
    val foo = 3
    fun bar(): Int {
        return 3
    }

    open fun openFun() {
    }

    abstract fun abstractFun()

    open val openVal = 3

    abstract var abstractVar: Int
}