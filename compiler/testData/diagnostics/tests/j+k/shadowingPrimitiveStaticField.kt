// FILE: B.kt

import aa.B

fun use() {
    // checking that CONST is of platform type
    B.CONST = null
    B.CONST?.length
    B.CONST.length
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