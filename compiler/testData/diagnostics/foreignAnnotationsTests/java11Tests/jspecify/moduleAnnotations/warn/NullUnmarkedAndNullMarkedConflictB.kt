// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE
// JSPECIFY_STATE: warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: null_marked_module/module-info.java

import org.jspecify.annotations.*;

@NullMarked
module null_marked_module {
    requires java9_annotations;
    exports conflictinglyannotatedpackage;
    exports unannotatedpackage;
}

// FILE: null_marked_module/conflictinglyannotatedpackage/package-info.java

@NullMarked
@NullUnmarked
package conflictinglyannotatedpackage;

import org.jspecify.annotations.*;

// FILE: null_marked_module/conflictinglyannotatedpackage/UnannotatedType.java

package conflictinglyannotatedpackage;

public interface UnannotatedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: null_marked_module/unannotatedpackage/ConflictinglyAnnotatedType.java

package unannotatedpackage;

import org.jspecify.annotations.*;

@NullMarked
@NullUnmarked
public interface ConflictinglyAnnotatedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: null_marked_module/unannotatedpackage/UnannotatedType.java

package unannotatedpackage;

import org.jspecify.annotations.*;

public interface UnannotatedType {
    @NullMarked
    @NullUnmarked
    public String conflictinglyAnnotatedProduce();
    @NullMarked
    @NullUnmarked
    public void conflictinglyAnnotatedConsume(String arg);
}

// FILE: null_marked_module/unannotatedpackage/UnannotatedTypeWithConflictinglyAnnotatedConstructor.java

package unannotatedpackage;

import org.jspecify.annotations.*;

public class UnannotatedTypeWithConflictinglyAnnotatedConstructor {
    @NullMarked
    @NullUnmarked
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
    a.unannotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    b.unannotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    c.conflictinglyAnnotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    unannotatedpackage.UnannotatedTypeWithConflictinglyAnnotatedConstructor(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
