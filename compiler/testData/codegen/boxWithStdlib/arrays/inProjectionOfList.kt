fun box(): String {
    val x: Array<List<*>> = array(listOf(1))
    val y : Array<in List<String>> = x

    if (y.size() != 1) return "fail 1"

    y[0] = listOf("OK")

    return x[0][0] as String
}