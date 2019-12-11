interface SuperInterface

open class SuperClass

abstract data class Base(val x: Int)

class Derived: Base(42)

data class Nasty(val z: Int, val y: Int): Base(z)

data class Complex(val y: Int): SuperInterface, SuperClass()

data class SubData(val sss: String) : Complex(42)
