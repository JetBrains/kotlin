// "Replace with 'NewClass'" "true"

class Outer {
    @Deprecated("", ReplaceWith("NewClass"))
    class OldClass

    class NewClass
}

fun foo(): Outer.OldClass<caret>? {
    return null
}
