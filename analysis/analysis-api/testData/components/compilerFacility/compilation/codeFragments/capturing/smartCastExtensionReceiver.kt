// MODULE: context

// FILE: context.kt
fun test() {
    dumbWith("Hello, world!") {
        if (this is String) {
            dumbWith(Foo()) {
                if (this is Foo) {
                    <caret_context>val x = 0
                }
            }
        }
    }
}

inline fun dumbWith(obj: Any?, block: Any?.() -> Unit) {
    obj.block()
}

class Foo {
    val foo: String = "foo"
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
length + foo.length