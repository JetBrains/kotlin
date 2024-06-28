// ISSUE: KT-46383

fun test(a: Any, b: Any, s: String, i: Int) {
    if (a is String && b is Int) {
        <!EQUALITY_NOT_APPLICABLE_WARNING!>a == b<!>
    }
    <!EQUALITY_NOT_APPLICABLE!>s == i<!>
}
