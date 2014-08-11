trait Trait : java.lang.Object {
    fun foo(): String = "239 " + toString()
}

class Impl : Trait, java.lang.Object() {
    override fun toString() = "Impl"
}

fun box(): String {
    return if ("239 Impl" == Impl().foo()) "OK" else "Fail"
}
