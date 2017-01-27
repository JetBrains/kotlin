fun foo() {
    // comment 1
    <caret>if (/* comment 2 */ true /* comment 3 */) {
        if (false) {
            // comment 4
            foo()
        }
    }
}