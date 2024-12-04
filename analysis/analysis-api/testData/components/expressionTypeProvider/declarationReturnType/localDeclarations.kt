fun test1() {
    val i = 1
    fun foo() = ""
    val f1 = {}
    val f2: String.(Int) -> String = { this + it }
}

fun <T> test2(t1: T) {
    val t2 = t1
    fun foo() = t1
    val f = { t1 }
}

fun test3(
    val i: Int = run {
        val j = 1
        j
    }
) {
}