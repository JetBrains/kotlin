// MODULE: context

// FILE: context.kt
import kotlin.reflect.KProperty

class Delegate(private var value: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        this.value = value
    }
}

fun test() {
    var x by Delegate("a")
    <caret_context>x
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
x = "O"
x + "K"