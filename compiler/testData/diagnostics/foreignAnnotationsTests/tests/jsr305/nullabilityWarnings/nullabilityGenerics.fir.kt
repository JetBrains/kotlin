// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: A.java
public class A<T> {
    public void foo(@MyNonnull T t) {
    }

    public @MyNullable String bar() {
        return null;
    }

    public @MyNullable T bam() {
        return null;
    }

    @MyNullable
    public <X> X baz() {
        return null;
    }

}
// FILE: main.kt
class X<T>(t: T?) {

    init {
        val a = A<T>()
        a.foo(<!ARGUMENT_TYPE_MISMATCH!>t<!>)

        val x: T = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>a.bam()<!>
        val y: T = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>a.baz<T>()<!>
    }
}

fun test() {
    val a = A<String?>()
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    val b: String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>a.bar()<!>
}
