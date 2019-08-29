// FIR_IGNORE
//                          Nothing?
//                          │ Nothing?
//                          │ │
fun <T> nullableValue(): T? = null

fun test() {
//      Int?
//      │   fun <T> nullableValue<Int>(): Int?
//      │   │
    val n = nullableValue<Int>()
//      Double?
//      │   fun <T> nullableValue<Double>(): Double?
//      │   │
    val x = nullableValue<Double>()
//      String?
//      │   fun <T> nullableValue<String>(): String?
//      │   │
    val s = nullableValue<String>()
}
