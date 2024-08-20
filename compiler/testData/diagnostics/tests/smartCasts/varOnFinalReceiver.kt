fun invokeLater(body: () -> Unit) {}

class Stable(val x: String? = "...")

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

fun test1() {
    val a: ImplBase = DefaultImpl()
    if (a is DefaultImpl) {
        a.x as String
        <!SMARTCAST_IMPOSSIBLE!>a.x<!>.length // ok
    }
}

fun test2() {
    var b: I = DefaultImpl()
    if (b is DefaultImpl) {
        b.x as String
        <!SMARTCAST_IMPOSSIBLE!>b.x<!>.length // ok

        b = makeUnstableImpl()
        b.x as String
        <!SMARTCAST_IMPOSSIBLE!>b.x<!>.length // bad
    }
}

fun test3() {
    val c: Any = DefaultImpl()
    if (c is DefaultImpl) {
        <!DEBUG_INFO_SMARTCAST!>c<!>.x as String
        <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>c<!>.x<!>.length // ok
    }
}

fun test4() {
    var b = DefaultImpl()
    invokeLater {
        b = DefaultImpl()
    }

    b.x as String
    <!SMARTCAST_IMPOSSIBLE!>b.x<!>.length // bad
}

fun test5() {
    var b = Stable()
    invokeLater {
        b = Stable()
    }

    b.x as String
    <!SMARTCAST_IMPOSSIBLE!>b.x<!>.length // bad
}

fun test6(){
    var b  = Stable()
    b.x as String
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>b.x<!>.length
        b = Stable()
    }
}

fun test7(){
    var b  = Stable()

    invokeLater {
        b.x as String
        <!SMARTCAST_IMPOSSIBLE!>b.x<!>.length
    }

    b = Stable()
}
