// ISSUE: KT-62998

// FILE: Foo.java
public class Foo {
    int a = 0;
}

// FILE: Main.kt
fun foo(foo: Foo?, arg: Int?) {
    foo?.a = null
    foo?.a = arg
}