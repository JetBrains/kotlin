// JSPECIFY_STATE: warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: conflictinglyannotatedpackage/package-info.java

@NullUnmarked
@NullMarked
package conflictinglyannotatedpackage;

import org.jspecify.annotations.*;

// FILE: conflictinglyannotatedpackage/UnannotatedType.java

package conflictinglyannotatedpackage;

public interface UnannotatedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: unannotatedpackage/ConflictinglyAnnotatedType.java

package unannotatedpackage;

import org.jspecify.annotations.*;

@NullUnmarked
@NullMarked
public interface ConflictinglyAnnotatedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: unannotatedpackage/UnannotatedType.java

package unannotatedpackage;

import org.jspecify.annotations.*;

public interface UnannotatedType {
    @NullUnmarked
    @NullMarked
    public String conflictinglyAnnotatedProduce();
    @NullUnmarked
    @NullMarked
    public void conflictinglyAnnotatedConsume(String arg);
}

// FILE: unannotatedpackage/UnannotatedTypeWithConflictinglyAnnotatedConstructor.java

package unannotatedpackage;

import org.jspecify.annotations.*;

public class UnannotatedTypeWithConflictinglyAnnotatedConstructor {
    @NullUnmarked
    @NullMarked
    public UnannotatedTypeWithConflictinglyAnnotatedConstructor(String arg) {}
}

// FILE: kotlin.kt

interface TestA: conflictinglyannotatedpackage.UnannotatedType {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun unannotatedProduce(): String?
}

interface TestB: unannotatedpackage.ConflictinglyAnnotatedType {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun unannotatedProduce(): String?
}

interface TestC: unannotatedpackage.UnannotatedType {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun conflictinglyAnnotatedProduce(): String?
}

fun test(
    a: conflictinglyannotatedpackage.UnannotatedType,
    b: unannotatedpackage.ConflictinglyAnnotatedType,
    c: unannotatedpackage.UnannotatedType
) {
    // jspecify_nullness_mismatch
    a.unannotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    b.unannotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    c.conflictinglyAnnotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    unannotatedpackage.UnannotatedTypeWithConflictinglyAnnotatedConstructor(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
