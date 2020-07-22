// FILE: A.java
public interface A {
    public class A_ {}
}

// FILE: 1.kt
interface B {
    class B_
}

class X: A {
    val a: <!OTHER_ERROR, OTHER_ERROR!>A_<!> = <!UNRESOLVED_REFERENCE!>A_<!>()
    val b: A.A_ = A.A_()

    companion object {
        val a: <!OTHER_ERROR, OTHER_ERROR!>A_<!> = <!UNRESOLVED_REFERENCE!>A_<!>()
    }
}

class Y: B {
    val a: <!OTHER_ERROR, OTHER_ERROR!>B_<!> = <!UNRESOLVED_REFERENCE!>B_<!>()
    val b: B.B_ = B.B_()

    companion object {
        val b: <!OTHER_ERROR, OTHER_ERROR!>B_<!> = <!UNRESOLVED_REFERENCE!>B_<!>()
    }
}
