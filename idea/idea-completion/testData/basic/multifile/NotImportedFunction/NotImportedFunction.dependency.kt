package test

fun globalFun1(): Int = 1
fun globalFun2(): Int = 1

object Some {
    fun globalFun3(): Int = 3
}

private fun globalFunPrivate() {}
