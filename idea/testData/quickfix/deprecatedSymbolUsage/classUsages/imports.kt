// "Replace with 'File'" "true"

@deprecated("", ReplaceWith("File", "java.io.File"))
class OldClass

fun foo(): OldClass<caret>? {
    return null
}
