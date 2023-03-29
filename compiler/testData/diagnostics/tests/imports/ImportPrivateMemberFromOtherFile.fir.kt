// FILE: A.kt

class A {
    private class Nested {
        object O1
    }
}

// FILE: B.java

public class B {
    private static class JC {
        public static class JC1 {
        }
    }
}

// FILE: C.kt

import A.Nested.*
import B.<!INVISIBLE_REFERENCE!>JC<!>.JC1

fun test() {
    <!INVISIBLE_REFERENCE!>O1<!>
    <!INVISIBLE_REFERENCE!>JC1<!>()
}
