// !LANGUAGE: +InlineClasses

inline class Z(val x: Int)

interface PublicMarker
interface ProtectedMarker
interface PrivateMarker


open class TestBasic(val z: Z) {
    constructor(z1: Z, publicMarker: PublicMarker) : this(z1)
    protected constructor(z: Z, protectedMarker: ProtectedMarker) : this(z)
    private constructor(z: Z, privateMarker: PrivateMarker) : this(z)
}

sealed class TestSealed(val z: Z) {
    class Case(z: Z) : TestSealed(z)
}

enum class TestEnum(val z: Z) {
    ANSWER(Z(42))
}

class TestInner {
    inner class Inner(val z: Z)
}