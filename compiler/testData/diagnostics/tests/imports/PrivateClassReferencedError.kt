// FILE: File1.kt
package pack1

public class SomeClass {
    private class N
    public open class PublicNested
}

// FILE: Main.kt
package a

import pack1.SomeClass.*

private class X : <!INVISIBLE_REFERENCE, INVISIBLE_MEMBER, FINAL_SUPERTYPE!>N<!>()