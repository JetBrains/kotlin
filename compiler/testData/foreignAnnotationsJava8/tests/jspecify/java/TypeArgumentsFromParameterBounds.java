import org.jspecify.annotations.*;

@DefaultNonNull
public class TypeArgumentsFromParameterBounds<T extends Object, E extends @Nullable Object, F extends @NullnessUnspecified Object> { }

@DefaultNonNull
class A {
    public void bar(TypeArgumentsFromParameterBounds<Test, Test, Test> a) {}
}

class B {
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
            A a, B b
    ) {
        a.bar(aNotNullNotNullNotNull);
        // jspecify_nullness_mismatch
        a.bar(aNotNullNotNullNull);
        // jspecify_nullness_mismatch
        a.bar(aNotNullNullNotNull);
        // jspecify_nullness_mismatch
        a.bar(aNotNullNullNull);

        // jspecify_nullness_mismatch
        b.bar(aNotNullNotNullNotNull);
        // jspecify_nullness_mismatch
        b.bar(aNotNullNotNullNull);
        b.bar(aNotNullNullNotNull);
        b.bar(aNotNullNullNull);
    }
}