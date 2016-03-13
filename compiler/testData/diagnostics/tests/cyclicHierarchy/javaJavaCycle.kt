// FILE: A.java

public interface A extends A {
    int getFoo();
}

// FILE: B.java

public class B {
    interface B1 extends B2 {
        int getFoo();
    }
    interface B2 extends B3 {
        int getFoo();
    }
    interface B3 extends B2 {
        int getFoo();
    }
}

// FILE: main.kt
fun foo() {
    object : A { override fun getFoo() = 1 }
    object : B.B1 { override fun getFoo() = 1 }
    object : B.B2 { override fun getFoo() = 1 }
    object : B.B3 { override fun getFoo() = 1 }
}

