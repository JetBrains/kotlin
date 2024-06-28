// ISSUE: KT-53460

fun test(a: String?, b: String?) {
    if (a == null || a == "foo") {
        when (b) {
            "abc" -> return
        }
    }

    a<!UNSAFE_CALL!>.<!>length // should be an error
    if (a == null || a == "bar") { // comparasion to null is not useless
        a<!UNSAFE_CALL!>.<!>length
    }
}
