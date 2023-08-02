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

enum class UserEnumeration {
    ENUM_ENTRY {
        // test 1: PTV is in consuming position (yield-case)
        fun testYield() {
            val buildee = build {
                yield(this@ENUM_ENTRY)
            }
            checkTypeEquality(this@ENUM_ENTRY, buildee.typeArgumentValue)
        }

        // test 2: PTV is in producing position (materialize-case)
        fun testMaterialize() {
            val buildee = build {
                fun <T> shareTypeInfo(from: T, to: T) {}
                shareTypeInfo(this@ENUM_ENTRY, materialize())
            }
            checkTypeEquality(this@ENUM_ENTRY, buildee.typeArgumentValue)
        }

        init {
            reference = this@ENUM_ENTRY
            testYield()
            testMaterialize()
        }
    };
}

fun box(): String {
    UserEnumeration.ENUM_ENTRY
    return "OK"
}
