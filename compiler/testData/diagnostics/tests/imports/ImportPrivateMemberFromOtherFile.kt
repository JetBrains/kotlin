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

import A.<!INVISIBLE_REFERENCE!>Nested<!>.*
import B.<!INVISIBLE_REFERENCE!>JC<!>.JC1

fun test() {
    <!INVISIBLE_MEMBER!>O1<!>
    <!INACCESSIBLE_TYPE!><!INVISIBLE_MEMBER!>JC1<!>()<!>
}