// SKIP_TXT
// ISSUE: KT-55932

fun test(x: String?): Int = <!TYPE_MISMATCH!>x?.length ?: "smth"<!>
