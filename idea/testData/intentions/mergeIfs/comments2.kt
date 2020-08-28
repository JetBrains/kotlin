fun foo() {
    // comment1
    <caret>if (true) /* comment2 */ /* comment3 */ {
        // comment4
        // comment5
        if (false) {
        }
        // comment6
        // comment7
    }
}