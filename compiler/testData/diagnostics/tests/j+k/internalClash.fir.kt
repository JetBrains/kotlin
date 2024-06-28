// FILE: B.java

public class B extends A {}

// FILE: box.kt

open class A {
    internal open val a: String = "Fail"
}

class C : B() {
    internal val <!VIRTUAL_MEMBER_HIDDEN!>a<!>: String = "OK"
}

fun box(): String {
    return C().a
}
