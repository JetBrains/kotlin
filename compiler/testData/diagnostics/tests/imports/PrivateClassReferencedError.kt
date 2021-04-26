// FILE: File1.kt
package pack1

public class SomeClass {
    private class N
    public open class PublicNested
}

// FILE: Main.kt
package a

import pack1.SomeClass.*

private class X : <!EXPOSED_FROM_PRIVATE_IN_FILE!><!FINAL_SUPERTYPE, INVISIBLE_MEMBER, INVISIBLE_REFERENCE!>N<!>()<!>
