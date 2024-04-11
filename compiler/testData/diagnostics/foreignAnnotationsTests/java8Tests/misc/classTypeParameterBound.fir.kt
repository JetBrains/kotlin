// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: ClassTypeParameterBound.java

import org.jetbrains.annotations.NotNull;

public class ClassTypeParameterBound <T extends @NotNull String> {
    ClassTypeParameterBound(T x) { }
    ClassTypeParameterBound() { }
}

// FILE: main.kt
fun main(x: ClassTypeParameterBound<<!UPPER_BOUND_VIOLATED!>String?<!>>, y: ClassTypeParameterBound<String>, a: String?, b: String) {
    val x2 = ClassTypeParameterBound<<!UPPER_BOUND_VIOLATED!>String?<!>>()
    val y2 = ClassTypeParameterBound<String>()

    val x3 = <!CANNOT_INFER_PARAMETER_TYPE!>ClassTypeParameterBound<!>(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    val y3 = ClassTypeParameterBound(b)

    val x4: ClassTypeParameterBound<<!UPPER_BOUND_VIOLATED!>String?<!>> = <!TYPE_MISMATCH!>ClassTypeParameterBound()<!>
    val y4: ClassTypeParameterBound<String> = ClassTypeParameterBound()
}
