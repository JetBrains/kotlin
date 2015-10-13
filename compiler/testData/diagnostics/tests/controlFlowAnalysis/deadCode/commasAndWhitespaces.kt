// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testCommasAndWhitespaces() {
    fun bar(i: Int, s: String, x: Any) {}

    <!UNREACHABLE_CODE!>bar(<!> 1 , todo() , <!UNREACHABLE_CODE!>"" )<!>
}

fun todo(): Nothing = throw Exception()



