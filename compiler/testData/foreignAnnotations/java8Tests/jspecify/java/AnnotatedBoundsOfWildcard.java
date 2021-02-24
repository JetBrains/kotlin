import org.jspecify.annotations.*;

@DefaultNonNull
public class AnnotatedBoundsOfWildcard {
    public void superAsIs(Test<? super @NullnessUnspecified Base, ? super @NullnessUnspecified Base, ? super @NullnessUnspecified Base> a) {}
    public void superNotNull(Test<? super Base, ? super Base, ? super Base> a) {}
    public void superNullable(Test<? super @Nullable Base, ? super @Nullable Base, ? super @Nullable Base> a) {}

    public void extendsAsIs(Test<? extends @NullnessUnspecified Base, ? extends @NullnessUnspecified Base, ? extends @NullnessUnspecified Base> a) {}
    public void extendsNotNull(Test<? extends Base, ? extends Base, ? extends Base> a) {}
    public void extendsNullable(Test<? extends @Nullable Base, ? extends @Nullable Base, ? extends @Nullable Base> a) {}

    public void noBounds(Test<@NullnessUnspecified ?, @NullnessUnspecified ?, @NullnessUnspecified ?> a) {}
}

class Base {}
class Derived extends Base {}

@DefaultNonNull
class Test<T extends Object, E extends @Nullable Object, F extends @NullnessUnspecified Object> { }

@DefaultNonNull
class Use {
    public void main(
            Test<Derived, Derived, Derived> aNotNullNotNullNotNull,
            Test<Derived, Derived, @Nullable Derived> aNotNullNotNullNull,
            Test<Derived, @Nullable Derived, Derived> aNotNullNullNotNull,
            Test<Derived, @Nullable Derived, @Nullable Derived> aNotNullNullNull,

            Test<Object, Object, Object> aObjectNotNullNotNullNotNull,
            Test<Object, Object, @Nullable Object> aObjectNotNullNotNullNull,
            Test<Object, @Nullable Object, Object> aObjectNotNullNullNotNull,
            Test<Object, @Nullable Object, @Nullable Object> aObjectNotNullNullNull,

            AnnotatedBoundsOfWildcard b
    ) {
        // jspecify_nullness_mismatch
        b.superAsIs(aObjectNotNullNotNullNotNull);
        // jspecify_nullness_mismatch
        b.superAsIs(aObjectNotNullNotNullNull);
        b.superAsIs(aObjectNotNullNullNotNull);
        b.superAsIs(aObjectNotNullNullNull);

        b.superNotNull(aObjectNotNullNotNullNotNull);
        b.superNotNull(aObjectNotNullNotNullNull);
        b.superNotNull(aObjectNotNullNullNotNull);
        b.superNotNull(aObjectNotNullNullNull);

        // jspecify_nullness_mismatch
        b.superNullable(aObjectNotNullNotNullNotNull);
        // jspecify_nullness_mismatch
        b.superNullable(aObjectNotNullNotNullNull);
        // jspecify_nullness_mismatch
        b.superNullable(aObjectNotNullNullNotNull);
        // jspecify_nullness_mismatch
        b.superNullable(aObjectNotNullNullNull);

        b.extendsAsIs(aNotNullNotNullNotNull);
        b.extendsAsIs(aNotNullNotNullNull);
        b.extendsAsIs(aNotNullNullNotNull);
        b.extendsAsIs(aNotNullNullNull);

        b.extendsNotNull(aNotNullNotNullNotNull);
        // jspecify_nullness_mismatch
        b.extendsNotNull(aNotNullNotNullNull);
        // jspecify_nullness_mismatch
        b.extendsNotNull(aNotNullNullNotNull);
        // jspecify_nullness_mismatch
        b.extendsNotNull(aNotNullNullNull);

        b.extendsNullable(aNotNullNotNullNotNull);
        b.extendsNullable(aNotNullNotNullNull);
        b.extendsNullable(aNotNullNullNotNull);
        b.extendsNullable(aNotNullNullNull);

        b.noBounds(aNotNullNotNullNotNull);
        b.noBounds(aNotNullNotNullNull);
        b.noBounds(aNotNullNullNotNull);
        b.noBounds(aNotNullNullNull);
    }
}
