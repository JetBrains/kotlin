// FILE: A.java

import kotlin.jvm.KotlinSignature;

public class A {
    @KotlinSignature("fun A(x: Int, y: String)")
    public A(int x, String y) {}
}

// FILE: 1.kt

val test = A(<!NAMED_ARGUMENTS_NOT_ALLOWED!>x<!> = 1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>y<!> = "2")
