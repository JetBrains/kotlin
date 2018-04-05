// WITH_RUNTIME

data class D(val x: List<String>)

fun box(): String {
    val data1 = D(listOf("1", "2"))
    val data2 = D(listOf("1", "2"))
    val data3 = D(listOf("2", "3"))
    if (data1 != data2) return "Failure 1"
    if (data1 == data3) return "Failure 2"
    if (data1.hashCode() != data2.hashCode()) return "Failure 3"
    if (data1.hashCode() == data3.hashCode()) return "Failure 4"
    return "OK"
}
