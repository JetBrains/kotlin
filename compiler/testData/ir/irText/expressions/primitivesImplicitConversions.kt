// IGNORE_BACKEND_K2: ANY
// FIR status: KT-46419, ILT conversions to Byte and Short are not supported by design

// IGNORE_BACKEND_K1: JS_IR

// KT-61141: Extra invocations of toLong(), toShort(), etc.. after unaryMinus()
// IGNORE_BACKEND_K1: NATIVE

val test1: Long = 42
val test2: Short = 42
val test3: Byte = 42
val test4: Long = 42.unaryMinus()
val test5: Short = 42.unaryMinus()
val test6: Byte = 42.unaryMinus()

fun test() {
    val test1: Int? = 42
    val test2: Long = 42
    val test3: Long? = 42
    val test4: Long? = -1
    val test5: Long? = 1.unaryMinus()
    val test6: Short? = 1.unaryMinus()
    val test7: Byte? = 1.unaryMinus()
}

fun testImplicitArguments(x: Long = 1.unaryMinus()) {}

class TestImplicitArguments(val x: Long = 1.unaryMinus())
