// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_EXTRA_CHECKERS
// Fixpoint generic in Java: Enum<T extends Enum<T>>
fun test(a : java.lang.annotation.RetentionPolicy) {

}

fun test() {
  java.util.Collections.<!CANNOT_INFER_PARAMETER_TYPE!>emptyList<!>()
  val a : Collection<String>? = java.util.Collections.emptyList()
}

fun test(a : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<Int><!>) {

}

fun test(a : java.util.ArrayList<Int>) {

}

fun test(a : java.lang.Class<Int>) {

}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, nullableType, propertyDeclaration */
