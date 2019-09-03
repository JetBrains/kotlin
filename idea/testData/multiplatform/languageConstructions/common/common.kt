package sample

expect class A {
    fun commonFun()
    val x: Int
    val y: Double
    val z: String
}

fun getCommonA(): A = null!!