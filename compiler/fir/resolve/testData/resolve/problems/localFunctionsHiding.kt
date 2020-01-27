interface A {
    val x: Int
}

fun test_1() {
    fun Int.transform(): Int = 1
    fun A.transform(): Int {
        return x.transform()
    }

    val y = 1
    y.transform()
}

fun test_2() {
    fun Int.transform(): Int = 1
    fun A.transformX(): Int {
        return x.transform()
    }

    val y = 1
    y.transform()
}