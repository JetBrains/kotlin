fun test() {
    // Comment 1
    loop@ while (true) {
        // Comment 2
        for (i in -10..10) {
            // Comment 3
            if (i < 0) {
                // Comment 4
                if (i < -5) {
                    break
                } else {
                    // Comment 5
                    continue@loop
                }
            } else {
                // Comment 6
                <caret>if (i == 0) {
                    i.hashCode()
                    // Comment 7
                    break
                } else if (i > 5) {
                    // Comment 8
                    i.hashCode()
                } else {
                    // Comment 9
                    continue
                }
            }
        }
    }
}