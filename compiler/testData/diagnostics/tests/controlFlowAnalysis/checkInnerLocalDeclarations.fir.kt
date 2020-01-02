package c

fun test() {
    val x = 10
    fun inner1() {
        fun inner2() {
            fun inner3() {
                val y = x
            }
        }
    }
}