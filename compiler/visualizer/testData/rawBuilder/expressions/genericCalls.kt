//                            Nothing?
//                            │
fun <T> nullableValue(): T? = null

fun test() {
//      Int?
//      │   fun <T> nullableValue(): Int?
//      │   │
    val n = nullableValue<Int>()
//      Double?
//      │   fun <T> nullableValue(): Double?
//      │   │
    val x = nullableValue<Double>()
//      String?
//      │   fun <T> nullableValue(): String?
//      │   │
    val s = nullableValue<String>()
}
