data class Some(val first: Int, val second: Double, val third: String)

fun foo(some: Some) {
    var (x, y, z: String) = some

    x++
    y *= 2.0
    z = ""
}