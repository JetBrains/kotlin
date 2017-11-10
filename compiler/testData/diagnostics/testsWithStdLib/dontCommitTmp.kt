val flag: Boolean = false

fun test1(x: Int?) {
    var z: Int? = null

    z = x ?: if (flag) 42 else 239

    val y = x ?: 42

    z.inc()
    y.inc()

}