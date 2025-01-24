// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: strict
// JSPECIFY_STATE: strict

// FILE: api/package-info.java
@org.jspecify.annotations.NullMarked
package api;

// FILE: api/Foo.java
package api;

public interface Foo<@org.jspecify.annotations.Nullable T> {
    void bar(T t);
}

// FILE: main.kt

import api.*

class FooImpl : Foo<String?> {
    override fun bar(s: String?) {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl2<!> : Foo<String?> {
    // jspecify_nullness_mismatch
    <!NOTHING_TO_OVERRIDE!>override<!> fun bar(s: String) {}
}
