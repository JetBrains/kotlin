// FILE: AC.kt

interface A {
    val a: Int
}

// FILE: B.java

public abstract class B implements A {
}

// FILE: C.kt

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class C<!> : B()

fun main() {
    C().a
}