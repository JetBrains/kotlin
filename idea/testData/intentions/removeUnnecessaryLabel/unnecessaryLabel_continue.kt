fun foo() {
    @loop do {
        continue@<caret>loop
    } while (false)
}
