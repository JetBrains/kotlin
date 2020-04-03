/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 *  - type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 */

// KT-7857: when exhaustiveness does not take previous nullability checks into account
enum class X { A, B }
fun foo(arg: X?): Int {
    if (arg != null) {
        return when (<!DEBUG_INFO_SMARTCAST!>arg<!>) {
            X.A -> 1
            X.B -> 2
            // else or null branch should not be required here!
        }
    } 
    else {
        return 0
    }
}