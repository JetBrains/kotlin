// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    // Like whileTrue but 2 == 2 is in use
    while(<!NON_TRIVIAL_BOOLEAN_CONSTANT!>2 == 2<!>) {
        p!!.length
        if (x()) break
    }
    // Smart cast should not work in this case, see KT-6284
    return p<!UNSAFE_CALL!>.<!>length
}
