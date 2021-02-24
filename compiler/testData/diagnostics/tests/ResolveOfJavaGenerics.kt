// !WITH_NEW_INFERENCE
// Fixpoint generic in Java: Enum<T extends Enum<T>>
fun test(<!UNUSED_PARAMETER!>a<!> : java.lang.annotation.RetentionPolicy) {

}

fun test() {
  java.util.Collections.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>emptyList<!>()
  val <!UNUSED_VARIABLE!>a<!> : Collection<String>? = java.util.Collections.emptyList()
}

fun test(<!UNUSED_PARAMETER!>a<!> : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<Int><!>) {

}

fun test(<!UNUSED_PARAMETER!>a<!> : java.util.ArrayList<Int>) {

}

fun test(<!UNUSED_PARAMETER!>a<!> : java.lang.Class<Int>) {

}
