interface I
class C<T>

private inline fun <reified T> C<T>.f() = object : I {
    val unused = T::class
}

fun box(): String {
    val t1 = C<String>().f()
    val t2 = C<String>().f()
    arrayOf(t1, t2)
    return "OK"
}
