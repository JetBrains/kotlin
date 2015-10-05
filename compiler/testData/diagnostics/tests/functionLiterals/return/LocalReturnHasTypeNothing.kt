// !CHECK_TYPE

fun test() {
    run1 f@{
        <!UNREACHABLE_CODE!>checkSubtype<Nothing>(<!>return@f 1<!UNREACHABLE_CODE!>)<!>
    }
}

fun <T> run1(f: () -> T): T { return f() }