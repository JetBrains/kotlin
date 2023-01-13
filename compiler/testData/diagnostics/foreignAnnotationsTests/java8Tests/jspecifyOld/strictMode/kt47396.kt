// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: Foo.java
import org.jspecify.nullness.*;

@NullMarked
public interface Foo {
    void test(Bar<?> list);
}

// FILE: Bar.java
import org.jspecify.nullness.*;

@NullMarked
public interface Bar<E extends @Nullable Object> {}

// FILE: main.kt
fun test(foo: Foo, bar: Bar<String?>) {
    foo.test(<!DEBUG_INFO_EXPRESSION_TYPE("Bar<kotlin.String?>")!>bar<!>)
}
