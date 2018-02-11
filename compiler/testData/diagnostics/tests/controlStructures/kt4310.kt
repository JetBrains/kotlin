// !WITH_NEW_INFERENCE
package f

fun test(a: Boolean, b: Boolean): Int {
    return <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>if(a) {
        1
    } else {
        <!OI;TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (b) {
            3
        }<!>
    }<!>
}