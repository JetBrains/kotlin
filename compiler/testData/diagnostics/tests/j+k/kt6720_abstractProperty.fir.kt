// FILE: AC.kt

interface A {
    val a: Int
}

// FILE: B.java

public abstract class B implements A {
}

// FILE: C.kt

class C : B()

fun main() {
    C().a
}