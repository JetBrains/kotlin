open class VeryBase

open class Base : VeryBase()

class Derived : Base() {
    fun original(): VeryBase = this
}

class Another : Base()

fun foo(d: Derived, a: Another?): Base? {
    // d is compared with d.original(): VeryBase but should retain its own type
    if (d.original() != d) return null
    // ad should be of type Base, not VeryBase
    val ad = a ?: d
    return ad
}