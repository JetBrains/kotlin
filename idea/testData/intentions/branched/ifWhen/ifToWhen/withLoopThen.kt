fun test() {
    // Loop comment
    for (i in -2..2) {
        // Some comment
        <caret>if (i < 0) {
            // Very important comment
            break
        }
        else {
            // More comments
            i.hashCode()
        }
    }
}