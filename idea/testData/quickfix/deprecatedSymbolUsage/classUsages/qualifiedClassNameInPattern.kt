// "Replace with 'java.io.File'" "true"

@Deprecated("", ReplaceWith("java.io.File"))
class OldClass

fun foo(): OldClass<caret>? {
    return null
}
