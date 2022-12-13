// JSPECIFY_STATE: warn
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: AnnotatedBoundsOfWildcard.java
import org.jspecify.annotations.*;

@NullMarked
public class AnnotatedBoundsOfWildcard {
    public void superAsIs(Test<? super Base, ? super @Nullable Base, ? super @NullnessUnspecified Base> a) {}
    public void superNotNull(Test<? super Base, ? super Base, ? super Base> a) {}
    public void superNullable(Test<? super @Nullable Base, ? super @Nullable Base, ? super @Nullable Base> a) {}

    public void extendsAsIs(Test<? extends Base, ? extends @Nullable Base, ? extends @NullnessUnspecified Base> a) {}
    public void extendsNotNull(Test<? extends Base, ? extends Base, ? extends Base> a) {}
    public void extendsNullable(Test<? extends @Nullable Base, ? extends @Nullable Base, ? extends @Nullable Base> a) {}

    public void noBounds(Test<? extends @NullnessUnspecified Object, ? extends @NullnessUnspecified Object, ? extends @NullnessUnspecified Object> a) {}
}

// FILE: Base.java
public class Base {}

// FILE: Derived.java
public class Derived extends Base {}

// FILE: Test.java
import org.jspecify.annotations.*;

@NullMarked
public class Test<T extends Object, E extends @Nullable Object, F extends @NullnessUnspecified Object> { }

// FILE: main.kt
fun main(
            aNotNullNotNullNotNull: Test<Derived, Derived, Derived>,
            aNotNullNotNullNull: Test<Derived, Derived, Derived?>,
            aNotNullNullNotNull: Test<Derived, Derived?, Derived>,
            aNotNullNullNull: Test<Derived, Derived?, Derived?>,

            aAnyNotNullNotNullNotNull: Test<Any, Any, Any>,
            aAnyNotNullNotNullNull: Test<Any, Any, Any?>,
            aAnyNotNullNullNotNull: Test<Any, Any?, Any>,
            aAnyNotNullNullNull: Test<Any, Any?, Any?>,

            b: AnnotatedBoundsOfWildcard
): Unit {
    b.superAsIs(aAnyNotNullNotNullNotNull)
    b.superAsIs(aAnyNotNullNotNullNull)
    b.superAsIs(aAnyNotNullNullNotNull)
    b.superAsIs(aAnyNotNullNullNull)

    b.superNotNull(aAnyNotNullNotNullNotNull)
    b.superNotNull(aAnyNotNullNotNullNull)
    b.superNotNull(aAnyNotNullNullNotNull)
    b.superNotNull(aAnyNotNullNullNull)

    b.superNullable(aAnyNotNullNotNullNotNull)
    b.superNullable(aAnyNotNullNotNullNull)
    b.superNullable(aAnyNotNullNullNotNull)
    b.superNullable(aAnyNotNullNullNull)

    b.extendsAsIs(aNotNullNotNullNotNull)
    b.extendsAsIs(aNotNullNotNullNull)
    b.extendsAsIs(aNotNullNullNotNull)
    b.extendsAsIs(aNotNullNullNull)

    b.extendsNotNull(aNotNullNotNullNotNull)
    b.extendsNotNull(aNotNullNotNullNull)
    b.extendsNotNull(aNotNullNullNotNull)
    b.extendsNotNull(aNotNullNullNull)

    b.extendsNullable(aNotNullNotNullNotNull)
    b.extendsNullable(aNotNullNotNullNull)
    b.extendsNullable(aNotNullNullNotNull)
    b.extendsNullable(aNotNullNullNull)

    b.noBounds(aNotNullNotNullNotNull)
    b.noBounds(aNotNullNotNullNull)
    b.noBounds(aNotNullNullNotNull)
    b.noBounds(aNotNullNullNull)
}
