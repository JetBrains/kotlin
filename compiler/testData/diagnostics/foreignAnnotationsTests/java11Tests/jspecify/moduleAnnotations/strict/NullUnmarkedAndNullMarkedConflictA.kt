// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE
// JSPECIFY_STATE: strict
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

@NullUnmarked
@NullMarked
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

@NullUnmarked
@NullMarked
public interface ConflictinglyAnnotatedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: null_marked_module/unannotatedpackage/UnannotatedType.java

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

// FILE: null_marked_module/unannotatedpackage/UnannotatedTypeWithConflictinglyAnnotatedConstructor.java

package unannotatedpackage;

import org.jspecify.annotations.*;

public class UnannotatedTypeWithConflictinglyAnnotatedConstructor {
    @NullUnmarked
    @NullMarked
    public UnannotatedTypeWithConflictinglyAnnotatedConstructor(String arg) {}
}

// FILE: kotlin.kt

interface TestA: conflictinglyannotatedpackage.UnannotatedType {
    override fun unannotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

interface TestB: unannotatedpackage.ConflictinglyAnnotatedType {
    override fun unannotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

interface TestC: unannotatedpackage.UnannotatedType {
    override fun conflictinglyAnnotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

fun test(
    a: conflictinglyannotatedpackage.UnannotatedType,
    b: unannotatedpackage.ConflictinglyAnnotatedType,
    c: unannotatedpackage.UnannotatedType
) {
    a.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    c.conflictinglyAnnotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    unannotatedpackage.UnannotatedTypeWithConflictinglyAnnotatedConstructor(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
