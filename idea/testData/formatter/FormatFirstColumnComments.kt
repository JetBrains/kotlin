package format.test

class LineComments {
    fun test() {
// Should not be formatted
  // Format
     // Format
        // Normal
    }
}

class KDocComments {
/**
 * Always ident for KDocs
 */
  /**
    * Format
     */
    fun test() {
/**
   * Always ident for KDocs
    */
 /**
        * Format
      */
   /**
        * Normal
         */
    }
}

class MultilineComments {
    fun test() {
/*
   * Should not be formatted
        */
  /*
     * Format
      */
        /*
         * Normal
         */
    }
}

// SET_TRUE: KEEP_FIRST_COLUMN_COMMENT