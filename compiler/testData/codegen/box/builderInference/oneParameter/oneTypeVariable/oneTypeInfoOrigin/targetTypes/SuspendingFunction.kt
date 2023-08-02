// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

// WITH_STDLIB

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = suspend {} as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    fun testBasicCase() {
        val arg: suspend () -> Unit = suspend {}
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<suspend () -> Unit>>(buildee)
    }

    fun testLiteral() {
        val buildee = build {
            yield(suspend {})
        }
        checkExactType<Buildee<suspend () -> Unit>>(buildee)
    }

    testBasicCase()
    testLiteral()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testBasicCase() {
        fun consume(arg: suspend () -> Unit) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<suspend () -> Unit>>(buildee)
    }

    fun testLiteral() {
        fun <T> shareTypeInfo(from: T, to: T) {}
        val buildee = build {
            shareTypeInfo(suspend {}, materialize())
        }
        checkExactType<Buildee<suspend () -> Unit>>(buildee)
    }

    testBasicCase()
    testLiteral()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
