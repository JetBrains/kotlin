// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn
// JSR305_MIGRATION_REPORT: strict

// FILE: A.java
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

public class A {
    @MyNullable
    public String foo() { return ""; }

    @MyMigrationNullable
    public String foo2() { return ""; }

    @Nullable
    public String foo3() { return ""; }

    public String foo4() { return ""; }

    public void bar(@MyNonnull String baz) { }

    public void bar2(@MyMigrationNonnull String baz) { }

    public void bar3(@Nonnull String baz) {}

    public void bar4(String baz) {}
}

// FILE: B.java
public class B extends A {
    @MyMigrationNullable
    public String foo() { return ""; }

    @MyNullable
    public String foo2() { return ""; }

    @MyNullable
    public String foo3() { return ""; }

    @MyNullable
    public String foo4() { return ""; }

    public void bar(@MyMigrationNonnull String baz) { }

    public void bar2(@MyNonnull String baz) { }

    public void bar3(@MyNonnull String baz) {}

    public void bar4(@MyNonnull String baz) {}
}

// FILE: C.java
public class C extends A {
    @MyMigrationNullable
    public String foo4() { return ""; }

    public void bar4(@MyMigrationNonnull String baz) {}
}

// FILE: main.kt
fun main(b: B, c: C) {
    b.foo()<!UNSAFE_CALL!>.<!>length
    b.foo()?.length
    b.foo2()<!UNSAFE_CALL!>.<!>length
    b.foo2()?.length
    b.foo3()<!UNSAFE_CALL!>.<!>length
    b.foo3()?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>b.foo4()<!>.length
    b.foo4()?.length

    b.bar(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b.bar("")
    b.bar2(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b.bar2("")
    b.bar3(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b.bar3("")
    b.bar4(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    b.bar4("")

    c.foo4()<!UNSAFE_CALL!>.<!>length
    c.foo4()?.length
    c.bar4(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    c.bar4("")
}
