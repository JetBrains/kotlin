// !LANGUAGE: +InlineClasses

annotation class Ann

inline class Z(val x: Int)

class Test @Ann constructor(@Ann val z: Z) {
    @Ann constructor(z: Z, @Ann a: Int) : this(z)
    @Ann private constructor(z: Z, @Ann s: String) : this(z)

    inner class Inner @Ann constructor(x: Int, @Ann val z2: Z, @Ann y: String)
}

sealed class Sealed @Ann constructor(@Ann val z: Z) {
    class Derived @Ann constructor(z: Z) : Sealed(z)

    inner class Inner @Ann constructor(x: Int, @Ann val z2: Z, @Ann y: String)
}