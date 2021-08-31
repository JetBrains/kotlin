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
fun main(x: ClassTypeParameterBound<String?>, y: ClassTypeParameterBound<String>, a: String?, b: String) {
    val x2 = ClassTypeParameterBound<String?>()
    val y2 = ClassTypeParameterBound<String>()

    val x3 = ClassTypeParameterBound(a)
    val y3 = ClassTypeParameterBound(b)

    val x4: ClassTypeParameterBound<String?> = ClassTypeParameterBound()
    val y4: ClassTypeParameterBound<String> = ClassTypeParameterBound()
}
