// LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated, -TypeEnhancementImprovementsInStrictMode
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: ClassTypeParameterBoundWithWarnings.java

import org.jetbrains.annotations.NotNull;

public class ClassTypeParameterBoundWithWarnings <T extends @NotNull String> {
    ClassTypeParameterBoundWithWarnings() { }
    ClassTypeParameterBoundWithWarnings(T x) { }
}

// FILE: main.kt
fun main(x: ClassTypeParameterBoundWithWarnings<<!UPPER_BOUND_VIOLATED!>String?<!>>, y: ClassTypeParameterBoundWithWarnings<String>, a: String?, b: String) {
    val x2 = ClassTypeParameterBoundWithWarnings<<!UPPER_BOUND_VIOLATED!>String?<!>>()
    val y2 = ClassTypeParameterBoundWithWarnings<String>()

    val x3 = <!CANNOT_INFER_PARAMETER_TYPE!>ClassTypeParameterBoundWithWarnings<!>(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    val y3 = ClassTypeParameterBoundWithWarnings(b)

    val x4: ClassTypeParameterBoundWithWarnings<<!UPPER_BOUND_VIOLATED!>String?<!>> = <!TYPE_MISMATCH!>ClassTypeParameterBoundWithWarnings()<!>
    val y4: ClassTypeParameterBoundWithWarnings<String> = ClassTypeParameterBoundWithWarnings()
}
