// !CHECK_TYPE

fun test() {
    run1 f@{
        <!UNREACHABLE_CODE!>checkSubtype<Nothing>(<!>return@f 1<!UNREACHABLE_CODE!>)<!>
    }
}

fun run1<T>(f: () -> T): T { return f() }