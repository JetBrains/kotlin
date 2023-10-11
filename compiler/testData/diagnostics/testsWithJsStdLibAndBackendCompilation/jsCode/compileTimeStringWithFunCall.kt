// FIR_DIFFERENCE
// The difference is okay: K1 and K2 report a bit differently
// ERROR_POLICY: SEMANTIC
@file:Suppress("NOTHING_TO_INLINE")

fun function1() = "function1"

inline fun inlineFunction1() = "inlineFunction1"

class Class {
    fun method1() = "method1"
    inline fun inlineMethod1() = "method1"

    fun testMethodFromThis() {
        js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var a = '${method1()}' + '" + method1() + "';"<!>)
    }

    fun testInlineMethodFromThis() {
        js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var a = '${inlineMethod1()}' + '" + inlineMethod1() + "';"<!>)
    }
}

fun testFunction() {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var a = '${function1()}' + '" + function1() + "';"<!>)
}

fun testInlineFunction() {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var a = '${inlineFunction1()}' + '" + inlineFunction1() + "';"<!>)
}

fun testMethod() {
    val c = Class()
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var a = '${c.method1()}' + '" + c.method1() + "';"<!>)
}

fun testInlineMethod() {
    val c = Class()
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var a = '${c.inlineMethod1()}' + '" + c.inlineMethod1() + "';"<!>)
}
