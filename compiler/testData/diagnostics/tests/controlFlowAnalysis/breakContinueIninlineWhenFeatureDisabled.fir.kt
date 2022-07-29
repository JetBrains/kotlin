// LANGUAGE: -BreakContinueInInlineLambdas


fun test() {
    var i = 0
    outer@ while (true) {
        i += 1
        inner@ for (j in 1..10) {
            {
                if (i == 2) {
                    continue@outer
                }

                if (i == 4) {
                    break@outer
                }

                if (j == 2) {
                    continue
                }

                if (j == 4) {
                    continue@inner
                }

                if (j == 6 && i == 1) {
                    break@inner
                }

                if (j == 6 && i == 3) {
                    break
                }
            }()
        }
    }
}