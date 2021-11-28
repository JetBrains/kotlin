//                          Nothing?
//                          │ Nothing?
//                          │ │
fun <T> nullableValue(): T? = null

fun test() {
//      Int?
//      │   fun <T> nullableValue<Int>(): T?
//      │   │
    val n = nullableValue<Int>()
//      Double?
//      │   fun <T> nullableValue<Double>(): T?
//      │   │
    val x = nullableValue<Double>()
//      String?
//      │   fun <T> nullableValue<String>(): T?
//      │   │
    val s = nullableValue<String>()
}
