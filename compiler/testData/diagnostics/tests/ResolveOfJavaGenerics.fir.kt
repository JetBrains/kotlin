// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_EXTENDED_CHECKERS
// Fixpoint generic in Java: Enum<T extends Enum<T>>
fun test(a : java.lang.annotation.RetentionPolicy) {

}

fun test() {
  java.util.Collections.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()
  val a : Collection<String>? = java.util.Collections.emptyList()
}

fun test(a : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<Int><!>) {

}

fun test(a : java.util.ArrayList<Int>) {

}

fun test(a : java.lang.Class<Int>) {

}
