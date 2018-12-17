// !LANGUAGE: -InlineConstVals
// FILE: test.kt
fun test1(a: A) = a.X
fun test2(a: A) = 1 + a.X
fun test3(a: A) = 1 < a.X

// FILE: A.java
public class A {
    public final int X = 42;
}

// @TestKt.class:
// 0 42
// 0 43
// 3 GETFIELD A.X
