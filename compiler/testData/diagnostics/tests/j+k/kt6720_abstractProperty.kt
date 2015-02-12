// FILE: AC.kt

trait A {
    val a: Int
}

// FILE: B.java

public abstract class B implements A {
}

// FILE: C.kt

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C<!> : B()

fun main() {
    C().a
}
