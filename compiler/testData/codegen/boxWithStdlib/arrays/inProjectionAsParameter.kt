fun test(y: Array<in Array<String>>) {
    y[0] = kotlin.array("OK")
}

fun box() : String {
    val x : Array<Array<*>> = kotlin.array(kotlin.array(1))
    test(x)
    return x[0][0] as String
}