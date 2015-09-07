// "Replace with 'NewClass'" "true"

class Outer {
    @deprecated("", ReplaceWith("NewClass"))
    class OldClass

    class NewClass
}

fun foo(): Outer.OldClass<caret>? {
    return null
}
