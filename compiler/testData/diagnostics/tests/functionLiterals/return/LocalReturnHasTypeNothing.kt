// !CHECK_TYPE

fun test() {
    run f@{
        <!UNREACHABLE_CODE!>checkSubtype<Nothing>(<!>return@f 1<!UNREACHABLE_CODE!>)<!>
    }
}
