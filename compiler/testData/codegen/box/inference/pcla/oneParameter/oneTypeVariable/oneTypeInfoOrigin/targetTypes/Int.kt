// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = 42 as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    fun testBasicCase() {
        val arg: Int = 42
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<Int>>(buildee)
    }

    fun testLiterals() {
        fun test1() {
            val buildee = build {
                yield(42)
            }
            checkExactType<Buildee<Int>>(buildee)
        }
        fun test2() {
            val buildee = build {
                yield(0x13)
            }
            checkExactType<Buildee<Int>>(buildee)
        }
        fun test3() {
            val buildee = build {
                yield(0b1000)
            }
            checkExactType<Buildee<Int>>(buildee)
        }

        test1()
        test2()
        test3()
    }

    testBasicCase()
    testLiterals()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testBasicCase() {
        fun consume(arg: Int) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<Int>>(buildee)
    }

    fun testLiterals() {
        fun test1() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(42, materialize())
            }
            checkExactType<Buildee<Int>>(buildee)
        }
        fun test2() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(0x13, materialize())
            }
            checkExactType<Buildee<Int>>(buildee)
        }
        fun test3() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(0b1000, materialize())
            }
            checkExactType<Buildee<Int>>(buildee)
        }

        test1()
        test2()
        test3()
    }

    testBasicCase()
    testLiterals()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
