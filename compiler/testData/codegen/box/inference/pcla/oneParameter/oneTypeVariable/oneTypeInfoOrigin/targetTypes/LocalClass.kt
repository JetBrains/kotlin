// IGNORE_BACKEND: JVM
/* ^ code compiled by legacy JVM backend fails in run-time with
 * NoSuchMethodError: LocalClassKt$testYield$3$buildee$1$Local: method <init>()V not found
 */

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
    fun testLocalClassOutsideBuilderArgument() {
        class Local
        reference = Local()

        val arg: Local = Local()
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<Local>>(buildee)
    }

    fun testLocalClassInsideBuilderArgument() {
        val buildee = build {
            class Local { fun localOnlyFunc() {} }
            reference = Local()

            val arg: Local = Local()
            yield(arg)
        }
        buildee.typeArgumentValue.localOnlyFunc()
    }

    fun testTypeInfoOriginInsideLocalClass() {
        val buildee = build {
            class Local {
                fun localOnlyFunc() {}

                fun initialize() {
                    reference = Local()

                    val arg: Local = Local()
                    yield(arg)
                }
            }
            Local().initialize()
        }
        buildee.typeArgumentValue.localOnlyFunc()
    }

    fun testThisExpression() {
        val buildee = build {
            class Local {
                fun localOnlyFunc() {}

                fun initialize() {
                    reference = this

                    yield(this)
                }
            }
            Local().initialize()
        }
        buildee.typeArgumentValue.localOnlyFunc()
    }

    testLocalClassOutsideBuilderArgument()
    testLocalClassInsideBuilderArgument()
    testTypeInfoOriginInsideLocalClass()
    testThisExpression()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testLocalClassOutsideBuilderArgument() {
        class Local
        reference = Local()

        fun consume(arg: Local) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<Local>>(buildee)
    }

    fun testLocalClassInsideBuilderArgument() {
        val buildee = build {
            class Local { fun localOnlyFunc() {} }
            reference = Local()

            fun consume(arg: Local) {}
            consume(materialize())
        }
        buildee.typeArgumentValue.localOnlyFunc()
    }

    fun testTypeInfoOriginInsideLocalClass() {
        val buildee = build {
            class Local {
                fun localOnlyFunc() {}

                fun initialize() {
                    reference = Local()

                    fun consume(arg: Local) {}
                    consume(materialize())
                }
            }
            Local().initialize()
        }
        buildee.typeArgumentValue.localOnlyFunc()
    }

    fun testThisExpression() {
        val buildee = build {
            class Local {
                fun localOnlyFunc() {}

                fun initialize() {
                    reference = this

                    fun <T> shareTypeInfo(from: T, to: T) {}
                    shareTypeInfo(this, materialize())
                }
            }
            Local().initialize()
        }
        buildee.typeArgumentValue.localOnlyFunc()
    }

    testLocalClassOutsideBuilderArgument()
    testLocalClassInsideBuilderArgument()
    testTypeInfoOriginInsideLocalClass()
    testThisExpression()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
