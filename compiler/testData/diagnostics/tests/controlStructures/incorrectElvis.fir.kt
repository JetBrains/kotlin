// SKIP_TXT
// ISSUE: KT-55932

fun test(x: String?): Int = <!RETURN_TYPE_MISMATCH!>x?.length ?: "smth"<!>
