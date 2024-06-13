interface I {
    val x: String?
}

abstract class ImplBase(override val x: String?) : I

class DefaultImpl : ImplBase("...")

class UnstableImpl : ImplBase("...") {
    override var x: String? = "..."
        get() = field.also { field = null }
}

fun makeUnstableImpl(): ImplBase = UnstableImpl()

fun main() {
    val a: ImplBase = DefaultImpl()
    if (a is DefaultImpl) {
        a.x as String
        a.x.length // ok
    }

    var b: I = DefaultImpl()
    if (b is DefaultImpl) {
        b.x as String
        b.x.length // ok

        b = makeUnstableImpl()
        b.x as String
        <!SMARTCAST_IMPOSSIBLE!>b.x<!>.length // bad
    }

    val c: Any = DefaultImpl()
    if (c is DefaultImpl) {
        c.x as String
        c.x.length // ok
    }
}
