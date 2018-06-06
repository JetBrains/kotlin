// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT warn
// JSR305_MIGRATION_REPORT ignore
// JSR305_SPECIAL_REPORT MyNullable:strict, MyMigrationNonnull:strict

// FILE: A.java

import javax.annotation.*;

public class A {
    @MyMigrationNullable public String field = null;

    @MyMigrationNullable
    public String foo(@MyMigrationNonnull String x, CharSequence y) {
        return "";
    }

    @MyMigrationNonnull
    public String bar() {
        return "";
    }

    @MyNullable public String field2 = null;
    @MyNullable
    public String foo2(@MyNonnull String x, CharSequence y) {
        return "";
    }

    @MyNonnull
    public String bar2() {
        return "";
    }
}

// FILE: main.kt

fun main(a: A) {
    a.foo("", null)?.length
    a.foo("", null).length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, "").length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    a.field.length

    a.foo2("", null)?.length
    a.foo2("", null)<!UNSAFE_CALL!>.<!>length
    a.foo2(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, "")<!UNSAFE_CALL!>.<!>length

    a.bar2().length
    a.bar2()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field2?.length
    a.field2<!UNSAFE_CALL!>.<!>length
}
