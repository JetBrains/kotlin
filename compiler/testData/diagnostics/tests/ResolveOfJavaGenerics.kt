// Fixpoint generic in Java: Enum<T extends Enum<T>>
fun test(<!UNUSED_PARAMETER!>a<!> : annotation.RetentionPolicy) {

}

fun test() {
  java.util.Collections.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()
  val <!UNUSED_VARIABLE!>a<!> : java.util.Collection<String>? = java.util.Collections.emptyList()
}

fun test(<!UNUSED_PARAMETER!>a<!> : java.lang.Comparable<Int>) {

}

fun test(<!UNUSED_PARAMETER!>a<!> : java.util.ArrayList<Int>) {

}

fun test(<!UNUSED_PARAMETER!>a<!> : java.lang.Class<Int>) {

}