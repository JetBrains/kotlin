// FILE: A.java
public interface A {
    public class A_ {}
}

// FILE: 1.kt
interface B {
    class B_
}

class X: A {
    val a: A_ = A_()
    val b: A.A_ = A.A_()

    companion object {
        val a: A_ = A_()
    }
}

class Y: B {
    val a: B_ = B_()
    val b: B.B_ = B.B_()

    companion object {
        val b: B_ = B_()
    }
}
