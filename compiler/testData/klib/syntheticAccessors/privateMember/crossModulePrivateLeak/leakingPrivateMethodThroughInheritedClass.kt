// IGNORE_BACKEND: JS_IR
// ^^^ This test is muted for JS because the produced IR can't be compiled to JS AST:
//     - Private member declaration `Parent.x` is moved to the top level by `PrivateMembersLowering`.
//     - `translateCall(IrCall, ...): JsExpression` processes `super.x()` call and attempts to
//       obtain a dispatch receiver, which is missing for top level declaration.

// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// ^^^ To be fixed in KT-72862: No function found for symbol

// IGNORE_BACKEND: JVM_IR

// MODULE: lib
// FILE: A.kt
open class Parent {
    private fun x() = "OK"
}

class Child : Parent() {
    @Suppress("INVISIBLE_REFERENCE")
    internal inline fun internalInlineMethod() = super.x()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return Child().internalInlineMethod()
}
