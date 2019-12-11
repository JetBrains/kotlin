//FILE: a/C.java
// KT-1431 StackOverflowException in IDE when using JavaFX builders
package a;

public class C<B extends C<B>> {
    public static C<?> create() { return null; }
    public C foo() {return null;}
}

//FILE: d.kt
package d

import a.C

fun test() {
    C.create().foo()
}
