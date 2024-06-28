// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = reference as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

private var reference: Any? = null
val <T> Buildee<T>.typeArgumentValue: T get() = reference as T

interface I1
interface I2
object A: I1, I2
object B: I1, I2

fun <T> select(vararg arg: T): T = arg[0]

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val intersection = select(A, B)
    val buildee = build {
        yield(intersection)
    }
    checkTypeEquality(intersection, buildee.typeArgumentValue)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    val intersection = select(A, B)
    val buildee = build {
        fun <T> shareTypeInfo(from: T, to: T) {}
        shareTypeInfo(intersection, materialize())
    }
    checkTypeEquality(intersection, buildee.typeArgumentValue)
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
