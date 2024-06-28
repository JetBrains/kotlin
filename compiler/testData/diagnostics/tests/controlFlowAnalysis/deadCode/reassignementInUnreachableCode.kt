// ISSUE: KT-47567

fun test(<!UNUSED_PARAMETER!>x<!>: Int)  {
    while (true)
        <!UNREACHABLE_CODE!>x =<!> break
}
