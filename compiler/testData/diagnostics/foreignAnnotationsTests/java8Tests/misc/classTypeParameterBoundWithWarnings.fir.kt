// !LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated, -TypeEnhancementImprovementsInStrictMode
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
fun main(x: ClassTypeParameterBoundWithWarnings<String?>, y: ClassTypeParameterBoundWithWarnings<String>, a: String?, b: String) {
    val x2 = ClassTypeParameterBoundWithWarnings<String?>()
    val y2 = ClassTypeParameterBoundWithWarnings<String>()

    val x3 = ClassTypeParameterBoundWithWarnings(a)
    val y3 = ClassTypeParameterBoundWithWarnings(b)

    val x4: ClassTypeParameterBoundWithWarnings<String?> = ClassTypeParameterBoundWithWarnings()
    val y4: ClassTypeParameterBoundWithWarnings<String> = ClassTypeParameterBoundWithWarnings()
}
