// FIR_IDENTICAL
// SKIP_TXT
// LANGUAGE: +PreferJavaFieldOverload
// FILE: a/JClass.java
package a;
public class JClass {
    public static int foo = 42;
}
// FILE: a.kt
package b
val foo = 42

// FILE: b.kt

import a.JClass.foo
import b.foo
fun test() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> }
