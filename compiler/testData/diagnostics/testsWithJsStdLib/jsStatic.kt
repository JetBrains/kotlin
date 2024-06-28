// FIR_IDENTICAL
// OPT_IN: kotlin.js.ExperimentalJsStatic
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// DIAGNOSTICS: -UNUSED_VARIABLE
class A {
    companion object {
        @JsStatic val a = 1;

        <!JS_STATIC_ON_CONST!>@JsStatic const val b<!> = 1;

        @JsStatic fun a1() {

        }

        <!JS_STATIC_ON_NON_PUBLIC_MEMBER!>@JsStatic private fun a2()<!> {

        }

        <!JS_STATIC_ON_NON_PUBLIC_MEMBER!>@JsStatic internal fun a3()<!> {

        }

        <!JS_STATIC_ON_OVERRIDE!>@JsStatic override fun toString(): String<!> = "TEST"
    }

    object A {
        <!JS_STATIC_NOT_IN_CLASS_COMPANION!>@JsStatic fun a2()<!> {

        }
    }

    fun test() {
        val s = object {
            <!JS_STATIC_NOT_IN_CLASS_COMPANION!>@JsStatic fun a3()<!> {

            }
        }
    }

    <!JS_STATIC_NOT_IN_CLASS_COMPANION!>@JsStatic fun a4()<!> {

    }
}

interface B {
    companion object {
        <!JS_STATIC_NOT_IN_CLASS_COMPANION!>@JsStatic fun a1()<!> {

        }
    }

    object A {
        <!JS_STATIC_NOT_IN_CLASS_COMPANION!>@JsStatic fun a2()<!> {

        }
    }

    fun test() {
        val s = object {
            <!JS_STATIC_NOT_IN_CLASS_COMPANION!>@JsStatic fun a3()<!> {

            }
        }
    }

    <!JS_STATIC_NOT_IN_CLASS_COMPANION!>@JsStatic fun a4()<!> {

    }
}