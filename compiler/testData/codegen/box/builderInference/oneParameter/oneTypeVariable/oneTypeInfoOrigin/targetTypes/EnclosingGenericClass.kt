// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = UserKlass<Placeholder>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass<T> {
    // test 1: PTV is in consuming position (yield-case)
    fun testYield() {
        fun testBasicCase() {
            val arg: UserKlass<T> = UserKlass()
            val buildee = build {
                yield(arg)
            }
            checkExactType<Buildee<UserKlass<T>>>(buildee)
        }

        fun testThisExpression() {
            val buildee = build {
                yield(this@UserKlass)
            }
            checkExactType<Buildee<UserKlass<T>>>(buildee)
        }

        testBasicCase()
        testThisExpression()
    }

    // test 2: PTV is in producing position (materialize-case)
    fun testMaterialize() {
        fun testBasicCase() {
            fun consume(arg: UserKlass<T>) {}
            val buildee = build {
                consume(materialize())
            }
            checkExactType<Buildee<UserKlass<T>>>(buildee)
        }

        fun testThisExpression() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(this@UserKlass, materialize())
            }
            checkExactType<Buildee<UserKlass<T>>>(buildee)
        }

        testBasicCase()
        testThisExpression()
    }
}

class Placeholder

fun box(): String {
    with(UserKlass<Placeholder>()) {
        testYield()
        testMaterialize()
    }
    return "OK"
}
