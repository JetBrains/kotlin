// "Update obsolete label syntax in whole project" "true"
// ERROR: Unresolved reference: @abc
// ERROR: Unresolved reference: @ann
// ERROR: Unresolved reference: @cde
// ERROR: Unresolved reference: @labeled
// ERROR: Unresolved reference: @loop
// ERROR: Unresolved reference: @loop2
// ERROR: Unresolved reference: @loop3
// ERROR: Unresolved reference: @loop4
// ERROR: Unresolved reference: @loop5
// ERROR: Unresolved reference: abc
// ERROR: Unresolved reference: cde
// ERROR: Unresolved reference: labeled
// ERROR: Unresolved reference: loop
// ERROR: Unresolved reference: loop1
// ERROR: Unresolved reference: loop2
// ERROR: Unresolved reference: loop2
// ERROR: Unresolved reference: loop3
// ERROR: Unresolved reference: loop4
// ERROR: Unresolved reference: loop5
// ERROR: Unresolved reference: noreferences
// ERROR: Unresolved reference: notLabelAnnotation
// ERROR: Unresolved reference: notLoop
// ERROR: The label '@abc' does not denote a loop
// ERROR: The label '@ann' does not denote a loop
// ERROR: The label '@cde' does not denote a loop
// ERROR: The label '@loop' does not denote a loop
// ERROR: The label '@loop2' does not denote a loop
// ERROR: The label '@loop3' does not denote a loop
// ERROR: The label '@loop4' does not denote a loop
// ERROR: The label '@loop5' does not denote a loop

fun run(block: () -> Unit) = block()

annotation class ann

@notLabelAnnotation class A {
    fun foo() {
        loop@
        for (i in 1..100) {
            /* comment */
            continue@loop
        }

        @noreferences for (i in 1..100) {
            val x = 1
        }

        @loop1 for (i in 1..100) {
            loop1@ for (j in 1..100) {
                continue@loop1
            }
        }

        loop2@ for (i in 1..100) {
            loop2@ for (j in 1..100) {
                break@loop2
            }
        }

        @[loop3] for (i in 1..100) {
            continue@loop3
        }

        run() labeled@ {
            return@labeled
        }

        @ann for (i in 1..100) {
            continue@ann
        }

        @ for (i in 1..100) {
            break@
        }

        @loop4() for (i in 1..100) {
            continue@loop4
        }

        @notLoop class Local

        @abc @cde for (i in 1..100) {
            break@cde
            break@abc
        }

        /* 123 */ loop5@    /* 456 */

        for (i in 1..100) {
            break@loop5
        } /* 789 */
    }
}
