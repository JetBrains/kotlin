// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = Out<UserSubklass>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class Out<out T>
open class UserKlass
open class UserSubklass: UserKlass()

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val arg: Out<UserKlass> = Out<UserSubklass>()
    val buildee = build {
        yield(arg)
    }
    checkExactType<Buildee<Out<UserKlass>>>(buildee)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: Out<UserKlass>) {}
    val buildee = build {
        consume(materialize())
    }
    checkExactType<Buildee<Out<UserKlass>>>(buildee)
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
