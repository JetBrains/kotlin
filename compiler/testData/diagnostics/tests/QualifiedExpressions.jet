package qualified_expressions

fun test(s: String?) {
   val <!UNUSED_VARIABLE!>a<!>: Int = <!TYPE_MISMATCH!>s?.length<!>
   val b: Int? = s?.length
   val <!UNUSED_VARIABLE!>c<!>: Int = s?.length ?: -11
   val <!UNUSED_VARIABLE!>d<!>: Int = s?.length ?: <!TYPE_MISMATCH!>"empty"<!>
   val e: String = <!TYPE_MISMATCH!>s?.length<!> ?: "empty"
   val <!UNUSED_VARIABLE!>f<!>: Int = s?.length ?: b ?: 1
   val <!UNUSED_VARIABLE!>g<!>: Boolean? = e.startsWith("s")//?.length
}

fun String.startsWith(<!UNUSED_PARAMETER!>s<!>: String): Boolean = true
