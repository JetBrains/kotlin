// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = UserEnumeration.ENUM_ENTRY as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

enum class UserEnumeration {
    ENUM_ENTRY;

    // test 1: PTV is in consuming position (yield-case)
    fun testYield() {
        fun testBasicCase() {
            val arg: UserEnumeration = ENUM_ENTRY
            val buildee = build {
                yield(arg)
            }
            checkExactType<Buildee<UserEnumeration>>(buildee)
        }

        fun testThisExpression() {
            val buildee = build {
                yield(this@UserEnumeration)
            }
            checkExactType<Buildee<UserEnumeration>>(buildee)
        }

        testBasicCase()
        testThisExpression()
    }

    // test 2: PTV is in producing position (materialize-case)
    fun testMaterialize() {
        fun testBasicCase() {
            fun consume(arg: UserEnumeration) {}
            val buildee = build {
                consume(materialize())
            }
            checkExactType<Buildee<UserEnumeration>>(buildee)
        }

        fun testThisExpression() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(this@UserEnumeration, materialize())
            }
            checkExactType<Buildee<UserEnumeration>>(buildee)
        }

        testBasicCase()
        testThisExpression()
    }
}

fun box(): String {
    with(UserEnumeration.ENUM_ENTRY) {
        testYield()
        testMaterialize()
    }
    return "OK"
}
