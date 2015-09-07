// "Replace with 'java.io.File'" "true"

@deprecated("", ReplaceWith("java.io.File"))
class OldClass

fun foo(): OldClass<caret>? {
    return null
}
