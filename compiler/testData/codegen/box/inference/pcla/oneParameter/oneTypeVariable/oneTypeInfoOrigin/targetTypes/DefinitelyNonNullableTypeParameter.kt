// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

class Context<T> {
    // test 1: PTV is in consuming position (yield-case)
    fun testYield() {
        val arg: T & Any = UserKlass() as (T & Any)
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<T & Any>>(buildee)
    }

    // test 2: PTV is in producing position (materialize-case)
    fun testMaterialize() {
        fun consume(arg: T & Any) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<T & Any>>(buildee)
    }
}

fun box(): String {
    with(Context<UserKlass?>()) {
        testYield()
        testMaterialize()
    }
    return "OK"
}
