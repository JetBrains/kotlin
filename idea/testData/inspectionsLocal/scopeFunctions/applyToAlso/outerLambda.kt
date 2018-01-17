// WITH_RUNTIME
// FIX: Convert to 'also'

val x = "".<caret>apply {
    "".apply {
        this.length
        length
    }
}
