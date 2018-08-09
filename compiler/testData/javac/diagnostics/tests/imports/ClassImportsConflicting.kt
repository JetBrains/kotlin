// SKIP_JAVAC
// FILE: b.kt
package b

object O {}

// FILE: c.kt
package c

object O {}

// FILE: a/a.java
package a;

import b.O;
import c.O;

public abstract class a {

    public O getO() { return null; }

}

// FILE: a.kt
package d

import a.a

class A : a()

fun test() = A().getO()
