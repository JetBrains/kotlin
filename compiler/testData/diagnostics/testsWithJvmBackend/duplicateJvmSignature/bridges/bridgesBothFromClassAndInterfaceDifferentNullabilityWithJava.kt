// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712
// FILE: A.java

import org.jetbrains.annotations.Nullable;

public class A<T> {
    public void foo(@Nullable T t) {
    }
}

// FILE: Main.kt

interface B<T> {
    fun foo(t: T) {
    }
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class C : A<String>(), B<String><!>
