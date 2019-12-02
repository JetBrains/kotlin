interface Ann {
    fun foo()
}

interface KC<T> {
    val x: T
}
fun <T> id(x: KC<T>): KC<T> = x
fun <T> KC<T>.idR(): KC<T> = this
val <T> KC<T>.idP: KC<T> get() = this

private fun getSetterInfos(kc: KC<out Ann>) {
    id(kc).x.foo()

    kc.idR().x.foo()
    kc.idP.x.foo()

    val x1 = id(kc)
    val x2 = kc.idR()
    val x3 = kc.idP
}
