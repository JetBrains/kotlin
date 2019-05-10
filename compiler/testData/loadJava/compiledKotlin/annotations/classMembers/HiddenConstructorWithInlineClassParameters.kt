// !LANGUAGE: +InlineClasses
package test

annotation class Ann

inline class Z(val x: Int)

class Test @Ann constructor(@Ann val z: Z) {
    @Ann constructor(z: Z, @Ann a: Int) : this(z)
    @Ann private constructor(z: Z, @Ann s: String) : this(z)
}

sealed class Sealed @Ann constructor(@Ann val z: Z) {
    class Derived @Ann constructor(z: Z) : Sealed(z)
}