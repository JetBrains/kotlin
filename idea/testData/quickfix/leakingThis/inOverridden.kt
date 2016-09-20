// "class org.jetbrains.kotlin.idea.quickfix.AddModifierFix" "false"
// ACTION: Add 'my =' to argument

open class My {
    init {
        register(<caret>this)
    }
}

class Derived : My()

fun register(my: My) {}