import org.jspecify.annotations.*;

@DefaultNonNull
public class WildcardsWithDefault {
    public void noBoundsNotNull(A<?, ?, ?> a) {}
    public void noBoundsNullable(A<@Nullable ?, @Nullable ?, @Nullable ?> a) {}
}

class A <T extends Object, E extends @Nullable Object, F extends @NullnessUnspecified Object> {}

@DefaultNonNull
class Use {
    public static void main(
            A<Object, Object, Object> aNotNullNotNullNotNull,
            A<Object, Object, @Nullable Object> aNotNullNotNullNull,
            A<Object, @Nullable Object, Object> aNotNullNullNotNull,
            A<Object, @Nullable Object, @Nullable Object> aNotNullNullNull,
            WildcardsWithDefault b
    ) {
        // jspecify_nullness_mismatch
        b.noBoundsNotNull(aNotNullNotNullNotNull);
        b.noBoundsNotNull(aNotNullNotNullNull);
        // jspecify_nullness_mismatch
        b.noBoundsNotNull(aNotNullNullNotNull);
        // jspecify_nullness_mismatch
        b.noBoundsNotNull(aNotNullNullNull);

        b.noBoundsNullable(aNotNullNotNullNotNull);
        b.noBoundsNullable(aNotNullNotNullNull);
        b.noBoundsNullable(aNotNullNullNotNull);
        b.noBoundsNullable(aNotNullNullNull);
    }
}