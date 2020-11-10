// JAVA_SOURCES: AnnotatedTypeParameter.java
// JSPECIFY_STATE strict

fun main(
          // jspecify_nullness_mismatch
          <!UNUSED_PARAMETER!>x1<!>: AnnotatedTypeParameter.Lib1<String?>,
          // jspecify_nullness_mismatch
          <!UNUSED_PARAMETER!>x2<!>: AnnotatedTypeParameter.Lib1<String?>,
          <!UNUSED_PARAMETER!>x3<!>: AnnotatedTypeParameter.Lib1<String?>
  ): Unit {
}