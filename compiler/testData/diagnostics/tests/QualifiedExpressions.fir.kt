package qualified_expressions

fun test(s: IntRange?) {
   val a: Int = <!INITIALIZER_TYPE_MISMATCH!>s?.start<!>
   val b: Int? = s?.start
   val c: Int = s?.start ?: -11
   val d: Int = <!TYPE_MISMATCH!>s?.start ?: "empty"<!>
   val e: String = <!TYPE_MISMATCH!>s?.start ?: "empty"<!>
   val f: Int = s?.endInclusive ?: b ?: 1
   val g: Boolean? = e.startsWith("s")//?.length
}

fun String.startsWith(s: String): Boolean = true
