// ISSUE: KT-10879
// FILE: A.java
public interface A {
    A getFoo();
}

// FILE: main.kt
interface B : A
interface C : A

fun test(x: A) {
    if (x !is C) return
    if (x is B) {
        x.foo.foo
        x.getFoo().foo
    }
}
