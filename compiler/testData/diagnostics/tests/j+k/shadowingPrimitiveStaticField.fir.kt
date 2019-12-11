// FILE: B.kt

import aa.B

fun use() {
    // checking that CONST is of platform type
    B.<!AMBIGUITY!>CONST<!> = null
    B.<!AMBIGUITY!>CONST<!>?.<!UNRESOLVED_REFERENCE!>length<!>
    B.<!AMBIGUITY!>CONST<!>.<!UNRESOLVED_REFERENCE!>length<!>
}

// FILE: aa/A.java
package aa;

public class A {
    public static int CONST = 3;
}

// FILE: aa/B.java
package aa;

public class B extends A {
    public static String CONST = null;
}