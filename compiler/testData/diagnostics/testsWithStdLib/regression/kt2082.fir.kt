fun foo(c : Collection<String>) = {
    c.filter{
        val s : String? = bar()
        if (s == null) false // here!
        <!INAPPLICABLE_CANDIDATE!>zoo<!>(s)
    }
}

fun bar() : String? = null
fun zoo(s : String) : Boolean = true