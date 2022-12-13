// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: WildcardsWithDefault.java
import org.jspecify.annotations.*;

@NullMarked
public class WildcardsWithDefault {
    public void noBoundsNotNull(A<?, ?, ?> a) {}
    public void noBoundsNullable(A<? extends @Nullable Object, ? extends @Nullable Object, ? extends @Nullable Object> a) {}
}

// FILE: A.java
import org.jspecify.annotations.*;

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
    b.noBoundsNotNull(aNotNullNotNullNull)
    b.noBoundsNotNull(aNotNullNullNotNull)
    b.noBoundsNotNull(aNotNullNullNull)

    b.noBoundsNullable(aNotNullNotNullNotNull)
    b.noBoundsNullable(aNotNullNotNullNull)
    b.noBoundsNullable(aNotNullNullNotNull)
    b.noBoundsNullable(aNotNullNullNull)
}
