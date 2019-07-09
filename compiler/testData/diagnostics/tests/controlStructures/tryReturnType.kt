// !WITH_NEW_INFERENCE
package a

fun foo() : Int {
    try {
        doSmth()
    }
    catch (e: Exception) {
        <!UNREACHABLE_CODE!>return<!> <!TYPE_MISMATCH!>""<!>
    }
    finally {
        return <!TYPE_MISMATCH!>""<!>
    }
}

fun bar() : Int =
    <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>try {
        <!OI;TYPE_MISMATCH!>doSmth()<!>
    }
    catch (e: Exception) {
        <!OI;TYPE_MISMATCH!>""<!>
    }
    finally {
        <!UNUSED_EXPRESSION!>""<!>
    }<!>


fun doSmth() {}
