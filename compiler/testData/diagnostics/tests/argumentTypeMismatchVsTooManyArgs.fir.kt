// ISSUE: KT-62541

fun foo(i1: Int) {}

fun test() {
    foo("",
        <!TOO_MANY_ARGUMENTS!>2<!>
    )
}