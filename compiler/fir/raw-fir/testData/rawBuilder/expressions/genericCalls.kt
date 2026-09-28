fun <T> nullableValue(): T? = null

fun test() {
    val n = nullableValue<Int>()
    val x = nullableValue<Double>()
    val s = nullableValue<String>()
}