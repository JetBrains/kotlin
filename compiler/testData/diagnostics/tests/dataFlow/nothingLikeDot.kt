fun Any?.dot(): Nothing {
    throw RuntimeException()
}

fun test(): String {
    val s: String? = ""
    <!UNREACHABLE_CODE!>return s.dot()<!>
}