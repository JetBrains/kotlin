@file:Suppress("DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE")

@Deprecated("")
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

@Deprecated("", level = DeprecationLevel.ERROR)
fun notHidden1() {}

@Deprecated("", level = DeprecationLevel.WARNING)
fun notHidden2() {}

@Deprecated("")
fun notHidden3() {}

@Deprecated("")
@DeprecatedSinceKotlin(hiddenSince = "999.999")
fun notHidden4() {}

interface A {
    @get:Deprecated("")
    @get:DeprecatedSinceKotlin(hiddenSince = "1.0")
    var hiddenGetter: Int

    @set:Deprecated("")
    @set:DeprecatedSinceKotlin(hiddenSince = "1.0")
    var hiddenSetter: Int

    @Deprecated("")
    @DeprecatedSinceKotlin(hiddenSince = "999.999")
    @get:Deprecated("")
    @get:DeprecatedSinceKotlin(hiddenSince = "1.0")
    var hiddenGetterButNotSetter: Int
}
