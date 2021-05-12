// JSPECIFY_STATE: strict
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: WildcardsWithDefault.java
import org.jspecify.nullness.*;

@NullMarked
public class WildcardsWithDefault {
    public void noBoundsNotNull(A<?, ?, ?> a) {}
    public void noBoundsNullable(A<? extends @Nullable Object, ? extends @Nullable Object, ? extends @Nullable Object> a) {}
}

// FILE: A.java
import org.jspecify.nullness.*;

public class A <T extends Object, E extends @Nullable Object, F extends @NullnessUnspecified Object> {}

// FILE: main.kt
fun main(
            aNotNullNotNullNotNull: A<Any, Any, Any>,
            aNotNullNotNullNull: A<Any, Any, Any?>,
            aNotNullNullNotNull: A<Any, Any?, Any>,
            aNotNullNullNull: A<Any, Any?, Any?>,
            b: WildcardsWithDefault
): Unit {
    b.noBoundsNotNull(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNullNull<!>)

    b.noBoundsNullable(aNotNullNotNullNotNull)
    b.noBoundsNullable(aNotNullNotNullNull)
    b.noBoundsNullable(aNotNullNullNotNull)
    b.noBoundsNullable(aNotNullNullNull)
}