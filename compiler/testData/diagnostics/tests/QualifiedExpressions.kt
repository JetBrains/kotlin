// !WITH_NEW_INFERENCE
package qualified_expressions

fun test(s: IntRange?) {
   val a: Int = <!TYPE_MISMATCH!>s?.start<!>
   val b: Int? = s?.start
   val c: Int = s?.start ?: -11
   val d: Int = <!TYPE_MISMATCH{NI}!>s?.start ?: <!TYPE_MISMATCH{OI}!>"empty"<!><!>
   val e: String = <!TYPE_MISMATCH{NI}!><!TYPE_MISMATCH{OI}!>s?.start<!> ?: "empty"<!>
   val f: Int = s?.endInclusive ?: b ?: 1
   val g: Boolean? = e.startsWith("s")//?.length
}

fun String.startsWith(s: String): Boolean = true
