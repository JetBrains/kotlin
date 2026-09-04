// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FILE: J.java
public class J {
    private String foo;

    public String getFoo() {
        return foo;
    }

    public String setFoo(String foo) {
        return this.foo = foo;
    }
}

// FILE: test.kt
class Foo(val j: J) {
    var bar by j::foo
}
