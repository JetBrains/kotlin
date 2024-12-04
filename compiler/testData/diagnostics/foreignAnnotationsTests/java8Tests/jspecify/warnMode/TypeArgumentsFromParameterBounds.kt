// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: TypeArgumentsFromParameterBounds.java
import org.jspecify.annotations.*;

@NullMarked
public class TypeArgumentsFromParameterBounds<T extends Object, E extends @Nullable Object, F extends @NullnessUnspecified Object> {}

// FILE: A.java
import org.jspecify.annotations.*;

@NullMarked
public class A {
    public void bar(TypeArgumentsFromParameterBounds<Test, Test, Test> a) {}
}

// FILE: B.java
import org.jspecify.annotations.*;

public class B {
    public void bar(TypeArgumentsFromParameterBounds<Test, Test, Test> a) {}
}

// FILE: Test.java
public class Test {}

// FILE: main.kt
fun main(
    aNotNullNotNullNotNull: TypeArgumentsFromParameterBounds<Test, Test, Test>,
    aNotNullNotNullNull: TypeArgumentsFromParameterBounds<Test, Test, Test?>,
    aNotNullNullNotNull: TypeArgumentsFromParameterBounds<Test, Test?, Test>,
    aNotNullNullNull: TypeArgumentsFromParameterBounds<Test, Test?, Test?>,
    a: A, b: B
): Unit {
    a.bar(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    a.bar(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>aNotNullNotNullNull<!>)
    // jspecify_nullness_mismatch
    a.bar(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>aNotNullNullNotNull<!>)
    // jspecify_nullness_mismatch
    a.bar(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>aNotNullNullNull<!>)

    b.bar(aNotNullNotNullNotNull)
    b.bar(aNotNullNotNullNull)
    b.bar(aNotNullNullNotNull)
    b.bar(aNotNullNullNull)
}