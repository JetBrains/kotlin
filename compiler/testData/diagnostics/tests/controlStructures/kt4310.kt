// !WITH_NEW_INFERENCE
package f

fun test(a: Boolean, b: Boolean): Int {
    return if(a) {
        1
    } else <!NI;TYPE_MISMATCH!>{
        <!OI;TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (b) {
            3
        }<!>
    }<!>
}