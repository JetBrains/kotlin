// FILE: Foo.java
public class Foo {
    public String bar;

    private Foo(String bar) {
        this.bar = bar;
    }

    public static Foo create(String bar) {
        return new Foo(bar);
    }
}

// FILE: Test.kt
fun test() {
    val foo = Foo.create(null)
    foo?.bar.let {
        // Error, foo?.bar is nullable
        it<!UNSAFE_CALL!>.<!>length
        // Foo is nullable but flexible, so call is considered safe here
        foo.bar.length
        // Correct
        foo?.bar?.length
    }
}