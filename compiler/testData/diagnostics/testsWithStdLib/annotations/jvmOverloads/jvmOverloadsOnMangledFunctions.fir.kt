// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +InlineClasses

inline class Z(val x: Int)

@JvmOverloads
fun testTopLevelFunction1(z: Z, x: Int = 0) {}

@JvmOverloads
fun testTopLevelFunction2(x: Int, z: Z = Z(0)) {}

@JvmOverloads
fun testTopLevelFunction3(x: Int = 0): Z = Z(x)

class C {
    @JvmOverloads
    constructor(i: Int, z: Z = Z(0))

    @JvmOverloads
    constructor(s: String, z: Z, i: Int = 0)

    @JvmOverloads
    fun testMemberFunction1(z: Z, x: Int = 0) {}

    @JvmOverloads
    fun testMemberFunction2(x: Int, z: Z = Z(0)) {}

    @JvmOverloads
    fun testMemberFunction3(x: Int = 0): Z = Z(x)
}