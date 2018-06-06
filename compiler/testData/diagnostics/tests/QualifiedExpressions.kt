// !WITH_NEW_INFERENCE
package qualified_expressions

fun test(s: IntRange?) {
   val <!UNUSED_VARIABLE!>a<!>: Int = <!TYPE_MISMATCH!>s?.start<!>
   val b: Int? = s?.start
   val <!UNUSED_VARIABLE!>c<!>: Int = s?.start ?: -11
   val <!UNUSED_VARIABLE!>d<!>: Int = <!NI;TYPE_MISMATCH!>s?.start ?: <!OI;TYPE_MISMATCH!>"empty"<!><!>
   val e: String = <!NI;TYPE_MISMATCH!><!OI;TYPE_MISMATCH!>s?.start<!> ?: "empty"<!>
   val <!UNUSED_VARIABLE!>f<!>: Int = s?.endInclusive ?: b ?: 1
   val <!UNUSED_VARIABLE!>g<!>: Boolean? = e.startsWith("s")//?.length
}

fun String.startsWith(<!UNUSED_PARAMETER!>s<!>: String): Boolean = true
