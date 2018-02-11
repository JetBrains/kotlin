// WITH_RUNTIME
// FIX: Convert to 'also'

val x = "".<caret>apply {
    this.length
    length
}
