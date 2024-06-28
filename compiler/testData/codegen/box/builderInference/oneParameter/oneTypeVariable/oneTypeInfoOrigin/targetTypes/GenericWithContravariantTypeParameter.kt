// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = In<UserSuperklass>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class In<in T>
open class UserSuperklass
open class UserKlass: UserSuperklass()

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val arg: In<UserKlass> = In<UserSuperklass>()
    val buildee = build {
        yield(arg)
    }
    checkExactType<Buildee<In<UserKlass>>>(buildee)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: In<UserKlass>) {}
    val buildee = build {
        consume(materialize())
    }
    checkExactType<Buildee<In<UserKlass>>>(buildee)
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
