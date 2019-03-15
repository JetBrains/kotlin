fun foo(i: Int) {
    when {
        // line
        1 -> 2

        // One line before

        2 -> 3

        // Many lines before


        3 -> 4


        /* Block */
        4 -> 5


        /*
          Block multiline
         */
        5 -> 6

        /** Doc */
        6 -> 7
    }

    when { // Assigned to entry
        1 -> 2
    }

    when {
        // Line
        else -> 8
    }

    when {
        /* Block */
        else -> 8
    }

    when {
        /**
         * Doc
         */
        else -> 8
    }

    when {
        1 ->
            2 // Two
        else ->
            43 // 42
    }
}