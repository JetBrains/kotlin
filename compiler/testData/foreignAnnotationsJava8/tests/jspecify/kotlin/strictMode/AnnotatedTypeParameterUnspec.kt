// JAVA_SOURCES: AnnotatedTypeParameterUnspec.java
// JSPECIFY_STATE strict

fun main(
          // jspecify_nullness_mismatch
          <!UNUSED_PARAMETER!>x1<!>: AnnotatedTypeParameterUnspec.Lib1<String?>,
          // jspecify_nullness_mismatch
          <!UNUSED_PARAMETER!>x2<!>: AnnotatedTypeParameterUnspec.Lib1<String?>,
          <!UNUSED_PARAMETER!>x3<!>: AnnotatedTypeParameterUnspec.Lib1<String?>
  ): Unit {
}