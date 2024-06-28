// FILE: a/x.java
package a;

public class x {}

// FILE: b/x.java
package b;

public class x {}

// FILE: c/d.java
package c;

import a.*;
import b.*;

public class d {
    public x x() { return null; }
} 

// FILE: c/c.kt
package c

import a.*
import b.*

fun test(): <!OVERLOAD_RESOLUTION_AMBIGUITY!>x<!> = d().<!MISSING_DEPENDENCY_CLASS!>x<!>()
