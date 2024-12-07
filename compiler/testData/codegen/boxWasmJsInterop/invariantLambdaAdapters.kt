// TARGET_BACKEND: WASM
// IGNORE_BACKEND: WASM
// Ignore reason KT-67438

fun f1(x: Number): String = x.toString()
fun f2(x: Number): String = x.toString()

fun floatLambda(f: (Float) -> String): String = js("f(2.5)")
fun byteLambda(f: (Byte) -> String): String =  js("f(2.5)")

fun box(): String {

    val f1ref = ::f1
    val resultAsFloat1 = floatLambda(f1ref)
    assertEquals(resultAsFloat1, "2.5")
    val resultAsByte1 = byteLambda(f1ref)
    assertEquals(resultAsByte1, "2")

    val f2ref = ::f2
    val resultAsByte2 = byteLambda(f2ref)
    assertEquals(resultAsByte2, "2")
    val resultAsFloat2 = floatLambda(f2ref)
    assertEquals(resultAsFloat2, "2.5")

    assertEquals(resultAsFloat1, resultAsFloat2)
    assertEquals(resultAsByte1, resultAsByte2)

    assertNotEquals(resultAsFloat1, resultAsByte1)
    assertNotEquals(resultAsFloat2, resultAsByte2)

    return "OK"
}