// JSPECIFY_STATE: warn
// FILE: A.java
import org.jspecify.nullness.*;

@NullMarked
public class A<T> {
    public void foo(@NullnessUnspecified T t) {}
    public <E> void bar(E e) {}
}

// FILE: B.java
import org.jspecify.nullness.*;
@NullMarked
public class B<T> {
    public void foo(T t) {}
    public <E> void bar(E e) {}
}

// FILE: Test.java
public class Test {}

// FILE: main.kt
// jspecify_nullness_mismatch, jspecify_nullness_mismatch
fun <T : Test> main(a1: A<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>Any?<!>>, a2: A<Test>, b1: B<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>Any?<!>>, b2: B<Test>, x: T): Unit {
    a1.foo(null)
    // jspecify_nullness_mismatch
    a1.bar<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>T?<!>>(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    a1.bar<T>(x)

    a2.foo(null)
    // jspecify_nullness_mismatch
    a2.bar<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>T?<!>>(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    a2.bar<T>(x)

    // jspecify_nullness_mismatch
    b1.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    b1.bar<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>T?<!>>(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    b1.bar<T>(x)

    // jspecify_nullness_mismatch
    b2.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    b2.bar<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>T?<!>>(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    b2.bar<T>(x)
}
