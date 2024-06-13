// ISSUE: KT-62998

// FILE: Foo.java
public class Foo {
    int a = 0;
}

// FILE: Main.kt
fun foo(foo: Foo?, arg: Int?) {
    foo?.a = <!NULL_FOR_NONNULL_TYPE!>null<!>
    foo?.a = <!ASSIGNMENT_TYPE_MISMATCH!>arg<!>
}
