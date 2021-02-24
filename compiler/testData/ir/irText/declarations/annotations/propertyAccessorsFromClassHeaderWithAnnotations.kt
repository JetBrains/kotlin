// FIR_IDENTICAL
annotation class A(val x: String)

class C(
    @get:A("C.x.get") val x: Int,
    @get:A("C.y.get") @set:A("C.y.set") var y: Int
)