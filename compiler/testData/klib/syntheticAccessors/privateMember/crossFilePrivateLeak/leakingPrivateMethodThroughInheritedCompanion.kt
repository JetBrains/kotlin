// IGNORE_BACKEND: JVM_IR, JS_IR

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
