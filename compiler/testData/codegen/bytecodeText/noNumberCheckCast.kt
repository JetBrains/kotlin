fun test() {
    val z : Int? = 1
    val r = z!! + 1
    stubPreventBoxingOptimization(z)
}

fun stubPreventBoxingOptimization(s: Int?) {
    s
}

//0 CHECKCAST java/lang/Number