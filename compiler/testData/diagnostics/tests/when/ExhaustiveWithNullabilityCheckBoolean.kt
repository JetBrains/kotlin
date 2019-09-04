/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-155, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 4
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 5
 *  - type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 */

// KT-7857: when exhaustiveness does not take previous nullability checks into account
fun foo(arg: Boolean?): Int {
    if (arg != null) {
        return when (<!DEBUG_INFO_SMARTCAST!>arg<!>) {
            true -> 1
            false -> 0
            // else or null branch should not be required here!
        }
    } 
    else {
        return -1
    }
}