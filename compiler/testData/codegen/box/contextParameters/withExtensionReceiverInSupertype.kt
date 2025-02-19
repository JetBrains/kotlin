// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters, +FunctionalTypeWithExtensionAsSupertype
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

class C(val a: String) {
    fun foo(): String {
        return a
    }
}

class Foo: context(C) C.(C)-> String {
    override fun invoke(p1: C, p2: C, p3: C): String {
        return p1.foo() + p2.foo() + p3.foo()
    }
}

fun box(): String {
    return Foo()(C("O"), C("K"), C(""))
}
