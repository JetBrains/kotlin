// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses
import kotlin.test.assertEquals

interface IFoo {
    fun fooFun(z: Z): Z
    var fooVar: Z
}

var global = Z(0U, 0)


@JvmInline
value class Z(val x1: UInt, val x2: Int) : IFoo {

    override fun fooFun(z: Z): Z = Z(z.x1 + x1, z.x2 + x2)

    override var fooVar: Z
        get() = Z(global.x1 + x1, global.x2 + x2)
        set(value) {
            global = Z(value.x1 + x1, value.x2 + x2)
        }

    fun barFun(z: Z): Z = Z(z.x1 * 100U + x1, z.x2 * 100 + x2)

    var barVar: Z
        get() = Z(global.x1 * 100U + x1, global.x2 * 100 + x2)
        set(value) {
            global = Z(value.x1 * 100U + x1, value.x2 * 100 + x2)
        }
}


fun box(): String {
    val fooFunR = Z::fooFun
    assertEquals(Z(53U, -53), fooFunR.callBy(mapOf(fooFunR.parameters[0] to Z(42U, -42), fooFunR.parameters[1] to Z(11U, -11))))

    val fooFunBR = Z(42U, -42)::fooFun
    assertEquals(Z(142U, -142), fooFunBR.callBy(mapOf(fooFunBR.parameters[0] to Z(100U, -100))))

    global = Z(0U, 0)
    val fooVarR = Z::fooVar
    assertEquals(Z(42U, -42), fooVarR.callBy(mapOf(fooVarR.parameters[0] to Z(42U, -42))))
    assertEquals(Z(42U, -42), fooVarR.getter.callBy(mapOf(fooVarR.getter.parameters[0] to Z(42U, -42))))
    fooVarR.setter.callBy(mapOf(fooVarR.setter.parameters[0] to Z(42U, -42), fooVarR.setter.parameters[1] to Z(1U, -1)))
    assertEquals(Z(43U, -43), global)

    global = Z(100U, -100)
    val fooVarBR = Z(42U, -42)::fooVar
    assertEquals(Z(142U, -142), fooVarBR.callBy(mapOf()))
    assertEquals(Z(142U, -142), fooVarBR.getter.callBy(mapOf()))
    fooVarBR.setter.callBy(mapOf(fooVarBR.setter.parameters[0] to Z(1U, -1)))
    assertEquals(Z(43U, -43), global)

    val barFunR = Z::barFun
    assertEquals(Z(1142U, -1142), barFunR.callBy(mapOf(barFunR.parameters[0] to Z(42U, -42), barFunR.parameters[1] to Z(11U, -11))))

    val barFunBR = Z(42U, -42)::barFun
    assertEquals(Z(2242U, -2242), barFunBR.callBy(mapOf(barFunBR.parameters[0] to Z(22U, -22))))

    global = Z(1U, -1)
    val barVarR = Z::barVar
    assertEquals(Z(142U, -142), barVarR.callBy(mapOf(barVarR.parameters[0] to Z(42U, -42))))
    assertEquals(Z(142U, -142), barVarR.getter.callBy(mapOf(barVarR.getter.parameters[0] to Z(42U, -42))))
    barVarR.setter.callBy(mapOf(barVarR.setter.parameters[0] to Z(42U, -42), barVarR.setter.parameters[1] to Z(3U, -3)))
    assertEquals(Z(342U, -342), global)

    global = Z(2U, -2)
    val barVarBR = Z(42U, -42)::barVar
    assertEquals(Z(242U, -242), barVarBR.callBy(mapOf()))
    assertEquals(Z(242U, -242), barVarBR.getter.callBy(mapOf()))
    barVarBR.setter.callBy(mapOf(barVarBR.setter.parameters[0] to Z(4U, -4)))
    assertEquals(Z(442U, -442), global)

    return "OK"
}