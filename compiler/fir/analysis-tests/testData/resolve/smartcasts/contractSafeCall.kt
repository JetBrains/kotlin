// ISSUE: KT-51796
// WITH_STDLIB

fun test(list: List<Any>?) {
    if (list?.isNullOrEmpty() == true) {
        return
    }
    list<!UNSAFE_CALL!>.<!>size // should be unsafe call
}
