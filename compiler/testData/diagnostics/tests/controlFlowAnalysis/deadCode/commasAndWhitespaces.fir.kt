// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testCommasAndWhitespaces() {
    fun bar(i: Int, s: String, x: Any) {}

    bar( 1 , todo() , "" )
}

fun todo(): Nothing = throw Exception()



