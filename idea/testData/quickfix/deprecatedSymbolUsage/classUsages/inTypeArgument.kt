// "Replace with 'NewClass'" "true"

@deprecated("", ReplaceWith("NewClass"))
class OldClass

class NewClass

fun foo(): List<OldClass<caret>>? {
    return null
}
