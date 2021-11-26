class Generic<P : Any>(val p: P)

class Host {
    fun t() {}
    val v = "OK"
}

fun box(): String {
    Generic(Host()).p::class
    (Generic(Host()).p::t)()
    return (Generic(Host()).p::v)()
}
