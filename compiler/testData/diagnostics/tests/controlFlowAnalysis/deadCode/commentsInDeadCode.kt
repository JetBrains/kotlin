package a

fun test1() {
    <!UNREACHABLE_CODE!>bar(<!>
        11,
        todo(),//comment1
        <!UNREACHABLE_CODE!>""//comment2
    )<!>
}

fun test2() {
    <!UNREACHABLE_CODE!>bar(<!>11, todo()/*comment1*/, <!UNREACHABLE_CODE!>""/*comment2*/)<!>
}
fun test3() {
    <!UNREACHABLE_CODE!>bar(<!>11, <!REDUNDANT_LABEL_WARNING!>l@<!>(todo()/*comment*/), <!UNREACHABLE_CODE!>"")<!>
}

fun todo(): Nothing = throw Exception()

fun bar(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>s<!>: String, <!UNUSED_PARAMETER!>a<!>: Any) {}


