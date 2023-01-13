// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: Foo.java
import org.jspecify.nullness.*;

@NullMarked
public class Foo<T extends @Nullable Object> {
    static <T extends Comparable> Foo<T> create() {
        return new Foo<>();
    }
}

// FILE: main.kt
fun test(): Foo<String> {
    return <!DEBUG_INFO_EXPRESSION_TYPE("Foo<kotlin.String>")!>Foo.create()<!>
}
