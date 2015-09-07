// "Replace with 'NewClass'" "true"

@deprecated("", ReplaceWith("NewClass"))
class OldClass

class NewClass

fun foo(): OldClass<caret>? {
    return null
}
