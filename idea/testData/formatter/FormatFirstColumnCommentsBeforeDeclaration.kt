package format.test

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