inline fun <reified T> f(x: T) {
    class X(val q: T)
    val xx = X(x)
    println(xx.q)
}

private val aaa: Int by lazy { 123 }

fun box(): String {

    f(aaa)

    return "OK"
}