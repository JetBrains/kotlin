// FILE: A.kt
interface A {
    var Int.zoo: Unit
    fun foo()
    fun Int.smth(): Short
    val foo: Int
    var bar: Long
    val Int.doo: String
}
®
class I(private val p: A) : A by p

// class: I
