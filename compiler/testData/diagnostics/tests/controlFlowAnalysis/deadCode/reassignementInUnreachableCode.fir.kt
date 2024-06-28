// ISSUE: KT-47567

fun test(x: Int)  {
    while (true)
        <!UNREACHABLE_CODE!><!VAL_REASSIGNMENT!>x<!> =<!> break
}
