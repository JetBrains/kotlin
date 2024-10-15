// RUN_PIPELINE_TILL: SOURCE
// ISSUE: KT-62541

fun foo(i1: Int) {}

fun test() {
    foo(<!TYPE_MISMATCH!>""<!>,
        <!TOO_MANY_ARGUMENTS!>2<!>
    )
}
