// !LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: ClassTypeParameterBoundWithWarnings.java

import org.jetbrains.annotations.NotNull;

public class ClassTypeParameterBoundWithWarnings <T extends @NotNull String> {
    ClassTypeParameterBoundWithWarnings() { }
    ClassTypeParameterBoundWithWarnings(T x) { }
}

// FILE: main.kt
fun main(x: ClassTypeParameterBoundWithWarnings<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>String?<!>>, y: ClassTypeParameterBoundWithWarnings<String>, a: String?, b: String) {
    val x2 = ClassTypeParameterBoundWithWarnings<String?>()
    val y2 = ClassTypeParameterBoundWithWarnings<String>()

    val x3 = ClassTypeParameterBoundWithWarnings(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a<!>)
    val y3 = ClassTypeParameterBoundWithWarnings(b)

    val x4: ClassTypeParameterBoundWithWarnings<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>String?<!>> = ClassTypeParameterBoundWithWarnings()
    val y4: ClassTypeParameterBoundWithWarnings<String> = ClassTypeParameterBoundWithWarnings()
}
