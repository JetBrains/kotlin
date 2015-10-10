// FILE: B.java

public abstract class B implements A {
    public int size = 1;
}

// FILE: main.kt

interface A {
    val size: Int
}

class C : B() {
    override val size: Int get() = 1
}

fun foo() {
    C().<!OVERLOAD_RESOLUTION_AMBIGUITY!>size<!>
}