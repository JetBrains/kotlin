fun box(): String {
    val x : Array<Array<*>> = array(array(1))
    val y : Array<in Array<String>> = x

    if (y.size() != 1) return "fail 1"

    y[0] = array("OK")

    return x[0][0] as String
}