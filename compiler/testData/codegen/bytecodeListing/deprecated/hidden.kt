@Deprecated("")
@Suppress("DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE")
@DeprecatedSinceKotlin(hiddenSince = "1.0")
fun hidden() {}

open class Base {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    open fun f() {}
}

class Derived : Base {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    constructor()

    override fun f() {}
}
