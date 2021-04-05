// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: ignore
// JSR305_MIGRATION_REPORT: ignore
// JSR305_SPECIAL_REPORT: MyNonnull:warn, MyMigrationNonnull:strict

// FILE: A.java
import javax.annotation.*;

public class A {
    @MyMigrationNullable public String field = null;

    @MyMigrationNullable
    public String foo(@MyMigrationNonnull String x, CharSequence y) {
        return "";
    }

    @MyNonnull
    @MyMigrationNonnull
    public String bar() {
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
}
