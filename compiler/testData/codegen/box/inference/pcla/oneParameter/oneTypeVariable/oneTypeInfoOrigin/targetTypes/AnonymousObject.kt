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

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    fun testAnonymousObjectOutsideBuilderArgument() {
        val obj = object {}
        reference = obj

        val buildee = build {
            yield(obj)
        }
        checkTypeEquality(obj, buildee.typeArgumentValue)
    }

    fun testAnonymousObjectInsideBuilderArgument() {
        val buildee = build {
            val obj = object { fun anonOnlyFunc() {} }
            reference = obj

            yield(obj)
        }
        buildee.typeArgumentValue.anonOnlyFunc()
    }

    fun testThisExpression() {
        val buildee = build {
            val obj = object {
                fun anonOnlyFunc() {}

                fun initialize() {
                    reference = this

                    yield(this)
                }
            }
            obj.initialize()
        }
        buildee.typeArgumentValue.anonOnlyFunc()
    }

    testAnonymousObjectOutsideBuilderArgument()
    testAnonymousObjectInsideBuilderArgument()
    testThisExpression()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testAnonymousObjectOutsideBuilderArgument() {
        val obj = object {}
        reference = obj

        val buildee = build {
            fun <T> shareTypeInfo(from: T, to: T) {}
            shareTypeInfo(obj, materialize())
        }
        checkTypeEquality(obj, buildee.typeArgumentValue)
    }

    fun testAnonymousObjectInsideBuilderArgument() {
        val buildee = build {
            val obj = object { fun anonOnlyFunc() {} }
            reference = obj

            fun <T> shareTypeInfo(from: T, to: T) {}
            shareTypeInfo(obj, materialize())
        }
        buildee.typeArgumentValue.anonOnlyFunc()
    }

    fun testThisExpression() {
        val buildee = build {
            val obj = object {
                fun anonOnlyFunc() {}

                fun initialize() {
                    reference = this

                    fun <T> shareTypeInfo(from: T, to: T) {}
                    shareTypeInfo(this, materialize())
                }
            }
            obj.initialize()
        }
        buildee.typeArgumentValue.anonOnlyFunc()
    }

    testAnonymousObjectOutsideBuilderArgument()
    testAnonymousObjectInsideBuilderArgument()
    testThisExpression()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
