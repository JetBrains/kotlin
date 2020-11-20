// JAVA_SOURCES: AnnotatedTypeParameter.java
// JSPECIFY_STATE strict

fun main(
          // jspecify_nullness_mismatch
          <!UNUSED_PARAMETER!>x1<!>: AnnotatedTypeParameter.Lib1<<!UPPER_BOUND_VIOLATED!>String?<!>>,
          // jspecify_nullness_mismatch
          <!UNUSED_PARAMETER!>x2<!>: AnnotatedTypeParameter.Lib2<<!UPPER_BOUND_VIOLATED!>String?<!>>,
          <!UNUSED_PARAMETER!>x3<!>: AnnotatedTypeParameter.Lib3<String?>
  ): Unit {
}