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
        a.foo(t)

        val x: T = a.bam()
        val y: T = a.baz<T>()
    }
}

fun test() {
    val a = A<String?>()
    a.foo(null)

    val b: String = a.bar()
}
