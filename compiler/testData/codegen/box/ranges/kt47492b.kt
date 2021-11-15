// WITH_STDLIB

fun a() = 5
fun b() = 1

fun p() {}

fun box(): String {
    try {
        val h1 = 1
        val h2 = 2L
        val h3 = 3L
    } catch (e: Exception) { throw e }
    var sum = 1
    for (i: Int? in a() downTo b())
        p()
    return "OK"
}