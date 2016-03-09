package format.test

// TODO: Comment on first column shouldn't be formatted, but now there's no way to adjust rule for the first comment in parent declaration.

class LineComments {
    // Should not be formatted
    // Format
    // Format
    fun test() {
    }
}

class MultilineComments {
    /*
       * Should not be formatted
       */
    /*
      * Format
      */
    fun test() {
    }
}

// SET_TRUE: KEEP_FIRST_COLUMN_COMMENT