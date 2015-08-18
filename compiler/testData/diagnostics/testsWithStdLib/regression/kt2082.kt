fun foo(c : Collection<String>) = {
    c.filter{
        val s : String? = bar()
        if (s == null) <!UNUSED_EXPRESSION!>false<!> // here!
        zoo(<!TYPE_MISMATCH!>s<!>)
    }
}

fun bar() : String? = null
fun zoo(<!UNUSED_PARAMETER!>s<!> : String) : Boolean = true