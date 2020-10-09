import org.jspecify.annotations.*;

@DefaultNonNull
public class TypeArgumentsFromParameterBounds<T extends Object, E extends @Nullable Object, F extends @NullnessUnspecified Object> { }

class A {
    public void bar(TypeArgumentsFromParameterBounds<@Nullable Test, @Nullable Test, @Nullable Test> a) {}
}

@DefaultNonNull
class B {
    public void bar(TypeArgumentsFromParameterBounds<Test, Test, Test> a) {}
}

class C {
    public void bar(TypeArgumentsFromParameterBounds<Test, Test, Test> a) {}
}

class Test {}

@DefaultNonNull
class Use {
    static public void main(
            TypeArgumentsFromParameterBounds<Test, Test, Test> aNotNullNotNullNotNull,
            TypeArgumentsFromParameterBounds<Test, Test, @Nullable Test> aNotNullNotNullNull,
            TypeArgumentsFromParameterBounds<Test, @Nullable Test, Test> aNotNullNullNotNull,
            TypeArgumentsFromParameterBounds<Test, @Nullable Test, @Nullable Test> aNotNullNullNull,
            A a, B b, C c
    ) {
        // jspecify_nullness_mismatch
        a.bar(aNotNullNotNullNotNull);
        // jspecify_nullness_mismatch
        a.bar(aNotNullNotNullNull);
        // jspecify_nullness_mismatch
        a.bar(aNotNullNullNotNull);
        a.bar(aNotNullNullNull);

        b.bar(aNotNullNotNullNotNull);
        // jspecify_nullness_mismatch
        b.bar(aNotNullNotNullNull);
        // jspecify_nullness_mismatch
        b.bar(aNotNullNullNotNull);
        // jspecify_nullness_mismatch
        b.bar(aNotNullNullNull);

        // jspecify_nullness_mismatch
        c.bar(aNotNullNotNullNotNull);
        // jspecify_nullness_mismatch
        c.bar(aNotNullNotNullNull);
        c.bar(aNotNullNullNotNull);
        c.bar(aNotNullNullNull);
    }
}