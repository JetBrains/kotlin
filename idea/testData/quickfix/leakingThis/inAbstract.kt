// "class org.jetbrains.kotlin.idea.quickfix.AddModifierFix" "false"
// ACTION: Add 'my =' to argument

abstract class My {
    init {
        register(<caret>this)
    }
}

fun register(my: My) {}