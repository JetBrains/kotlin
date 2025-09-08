// IGNORE_BACKEND: JS_IR
// ^^^ This test is muted for JS because the produced IR can't be compiled to JS AST:
//     - Private member declaration `Parent.x` is moved to the top level by `PrivateMembersLowering`.
//     - `translateCall(IrCall, ...): JsExpression` processes `super.x()` call and attempts to
//       obtain a dispatch receiver, which is missing for top level declaration.

// IGNORE_BACKEND: JVM_IR

// FILE: A.kt
open class Parent {
    private fun x() = "OK"
}

class ChildCompanion {
    internal companion object : Parent() {
        @Suppress("INVISIBLE_REFERENCE")
        internal inline fun internalInlineMethod() = super.x()
    }
}

// FILE: main.kt
fun box(): String {
    return ChildCompanion.internalInlineMethod()
}
